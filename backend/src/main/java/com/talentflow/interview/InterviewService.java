package com.talentflow.interview;

import com.talentflow.application.Application;
import com.talentflow.application.ApplicationAccess;
import com.talentflow.application.Stage;
import com.talentflow.audit.AuditService;
import com.talentflow.auth.CustomUserDetails;
import com.talentflow.common.BadRequestException;
import com.talentflow.common.ConflictException;
import com.talentflow.common.ResourceNotFoundException;
import com.talentflow.notification.DomainEvents;
import com.talentflow.user.Role;
import com.talentflow.user.User;
import com.talentflow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private static final long MAX_CALENDAR_RANGE_DAYS = 92;

    private final InterviewRepository interviewRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final ApplicationAccess applicationAccess;
    private final InterviewMapper mapper;
    private final AuditService auditService;
    private final ApplicationEventPublisher events;

    // ---------- planification ----------

    @Transactional
    public InterviewDto schedule(Long applicationId, InterviewRequest request, CustomUserDetails actor) {
        Application application = applicationAccess.findVisible(applicationId, actor);
        if (application.getCurrentStage() == Stage.REJECTED || application.getCurrentStage() == Stage.HIRED) {
            throw new BadRequestException("Cannot schedule an interview for a rejected or hired application");
        }

        Set<Long> ids = request.participantIds();
        List<User> participants = userRepository.findAllById(ids);
        if (participants.size() != ids.size()) {
            throw new BadRequestException("Unknown participant");
        }
        for (User participant : participants) {
            if (participant.getRole() == Role.CANDIDATE || !participant.isEnabled()) {
                throw new BadRequestException("Participants must be active staff members");
            }
        }

        Instant end = request.startAt().plus(Duration.ofMinutes(request.durationMin()));
        if (interviewRepository.countOverlapping(ids, request.startAt(), end) > 0) {
            throw new ConflictException("A participant already has an interview at this time");
        }

        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setStartAt(request.startAt());
        interview.setDurationMin(request.durationMin());
        interview.setMode(request.mode());
        interview.setLocationOrLink(request.locationOrLink());
        interview.setCreatedBy(userRepository.getReferenceById(actor.getId()));
        interview.setParticipants(new HashSet<>(participants));
        if (request.startAt().isBefore(Instant.now().plus(Duration.ofHours(24)))) {
            interview.setReminderSentAt(Instant.now());   // déjà dans les 24 h : l'invitation suffit
        }
        Interview saved = interviewRepository.saveAndFlush(interview);

        auditService.log(actor.getId(), "INTERVIEW_SCHEDULED", "Interview", saved.getId());
        events.publishEvent(new DomainEvents.InterviewScheduled(saved.getId(), actor.getId()));
        return mapper.toDto(saved);
    }

    @Transactional
    public InterviewDto cancel(Long interviewId, CustomUserDetails actor) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new BadRequestException("Only a scheduled interview can be cancelled");
        }
        interview.setStatus(InterviewStatus.CANCELLED);
        auditService.log(actor.getId(), "INTERVIEW_CANCELLED", "Interview", interviewId);
        events.publishEvent(new DomainEvents.InterviewCancelled(interviewId, actor.getId()));
        return mapper.toDto(interview);
    }

    // ---------- consultation ----------

    @Transactional(readOnly = true)
    public List<InterviewDto> listForApplication(Long applicationId, CustomUserDetails user) {
        applicationAccess.findVisible(applicationId, user);
        return interviewRepository.findByApplicationIdOrderByStartAtAsc(applicationId)
                .stream().map(mapper::toDto).toList();
    }

    /** Vue calendrier (RF-43) sur [from, to[ : la RH voit tout, un manager voit ses entretiens. */
    @Transactional(readOnly = true)
    public List<InterviewDto> calendar(Instant from, Instant to, CustomUserDetails user) {
        if (!to.isAfter(from)) {
            throw new BadRequestException("'to' must be after 'from'");
        }
        if (Duration.between(from, to).toDays() > MAX_CALENDAR_RANGE_DAYS) {
            throw new BadRequestException("The range must not exceed " + MAX_CALENDAR_RANGE_DAYS + " days");
        }
        List<Interview> interviews = user.getRole() == Role.MANAGER
                ? interviewRepository.findInRangeForUser(from, to, user.getId())
                : interviewRepository.findInRange(from, to);
        return interviews.stream().map(mapper::toDto).toList();
    }

    // ---------- feedback ----------

    /** Réservé aux participants de l'entretien, une fois celui-ci commencé (un feedback par entretien). */
    @Transactional
    public FeedbackDto submitFeedback(Long interviewId, FeedbackRequest request, CustomUserDetails user) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
        if (!isParticipant(interview, user)) {
            throw new ResourceNotFoundException("Interview not found");
        }
        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new BadRequestException("This interview was cancelled");
        }
        if (interview.getStartAt().isAfter(Instant.now())) {
            throw new BadRequestException("Feedback can only be submitted once the interview has started");
        }
        if (feedbackRepository.findByInterviewId(interviewId).isPresent()) {
            throw new ConflictException("A feedback has already been submitted for this interview");
        }

        Feedback feedback = new Feedback();
        feedback.setInterview(interview);
        feedback.setAuthor(userRepository.getReferenceById(user.getId()));
        feedback.setRating(request.rating().shortValue());
        feedback.setStrengths(request.strengths());
        feedback.setWeaknesses(request.weaknesses());
        feedback.setRecommendation(request.recommendation());
        Feedback saved = feedbackRepository.saveAndFlush(feedback);

        interview.setStatus(InterviewStatus.DONE);
        events.publishEvent(new DomainEvents.FeedbackSubmitted(interviewId, user.getId()));
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public FeedbackDto getFeedback(Long interviewId, CustomUserDetails user) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
        if (!canView(interview, user)) {
            throw new ResourceNotFoundException("Interview not found");
        }
        return feedbackRepository.findByInterviewId(interviewId)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("No feedback for this interview yet"));
    }

    // ---------- helpers ----------

    private boolean canView(Interview interview, CustomUserDetails user) {
        return switch (user.getRole()) {
            case ADMIN, HR -> true;
            case MANAGER -> isParticipant(interview, user) || isJobManager(interview, user);
            case CANDIDATE -> false;
        };
    }

    private static boolean isParticipant(Interview interview, CustomUserDetails user) {
        return interview.getParticipants().stream().anyMatch(p -> p.getId().equals(user.getId()));
    }

    private static boolean isJobManager(Interview interview, CustomUserDetails user) {
        User manager = interview.getApplication().getJobOffer().getManager();
        return manager != null && manager.getId().equals(user.getId());
    }
}
