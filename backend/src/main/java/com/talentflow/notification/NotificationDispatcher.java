package com.talentflow.notification;

import com.talentflow.application.Application;
import com.talentflow.application.ApplicationRepository;
import com.talentflow.application.Stage;
import com.talentflow.interview.Interview;
import com.talentflow.interview.InterviewRepository;
import com.talentflow.job.JobOffer;
import com.talentflow.user.Role;
import com.talentflow.user.User;
import com.talentflow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Compose et envoie les emails et notifications in-app liés au recrutement.
 * Un échec d'envoi d'email est journalisé mais n'annule jamais les notifications in-app.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private static final Duration REMINDER_HORIZON = Duration.ofHours(24);

    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final EmailRenderer renderer;
    private final AppMailProperties mailProps;

    // ---------- candidatures ----------

    @Transactional
    public void onApplicationSubmitted(Long applicationId) {
        applicationRepository.findById(applicationId).ifPresent(application -> {
            JobOffer job = application.getJobOffer();
            User candidate = application.getCandidate();

            email(candidate, EmailTemplate.APPLICATION_RECEIVED, job.getTitle(), vars(
                    "candidateName", fullName(candidate),
                    "jobTitle", job.getTitle(),
                    "appUrl", mailProps.baseUrl()));

            notifyStaff(users(job.getCreatedBy(), job.getManager()), null, "APPLICATION_RECEIVED",
                    "Nouvelle candidature de %s pour « %s »".formatted(fullName(candidate), job.getTitle()),
                    "APPLICATION", applicationId);
        });
    }

    @Transactional
    public void onStageChanged(Long applicationId, Stage to, Long actorId) {
        applicationRepository.findById(applicationId).ifPresent(application -> {
            JobOffer job = application.getJobOffer();
            User candidate = application.getCandidate();

            // Un retour en arrière vers "reçue" n'a pas de sens pour le candidat : pas d'email.
            if (to != Stage.RECEIVED) {
                email(candidate, EmailTemplate.STAGE_CHANGED, job.getTitle(), vars(
                        "candidateName", fullName(candidate),
                        "jobTitle", job.getTitle(),
                        "stageLabel", EmailLabels.stage(to),
                        "rejected", to == Stage.REJECTED,
                        "hired", to == Stage.HIRED));
            }

            notifyStaff(users(job.getManager(), job.getCreatedBy()), actorId, "STAGE_CHANGED",
                    "« %s » : %s - %s".formatted(job.getTitle(), fullName(candidate), EmailLabels.stage(to)),
                    "APPLICATION", applicationId);
        });
    }

    // ---------- entretiens ----------

    @Transactional
    public void onInterviewScheduled(Long interviewId, Long actorId) {
        interviewRepository.findById(interviewId).ifPresent(interview -> {
            emailInterview(interview, EmailTemplate.INTERVIEW_INVITATION);
            notifyStaff(interview.getParticipants(), actorId, "INTERVIEW_SCHEDULED",
                    "Entretien avec %s pour « %s » le %s".formatted(
                            fullName(interview.getApplication().getCandidate()),
                            interview.getApplication().getJobOffer().getTitle(),
                            formatWhen(interview.getStartAt())),
                    "INTERVIEW", interviewId);
        });
    }

    @Transactional
    public void onInterviewCancelled(Long interviewId, Long actorId) {
        interviewRepository.findById(interviewId).ifPresent(interview -> {
            emailInterview(interview, EmailTemplate.INTERVIEW_CANCELLED);
            notifyStaff(interview.getParticipants(), actorId, "INTERVIEW_CANCELLED",
                    "Entretien annulé : %s, « %s », le %s".formatted(
                            fullName(interview.getApplication().getCandidate()),
                            interview.getApplication().getJobOffer().getTitle(),
                            formatWhen(interview.getStartAt())),
                    "INTERVIEW", interviewId);
        });
    }

    @Transactional
    public void onFeedbackSubmitted(Long interviewId, Long authorId) {
        interviewRepository.findById(interviewId).ifPresent(interview -> {
            User author = userRepository.findById(authorId).orElse(null);
            if (author == null) {
                return;
            }
            notifyStaff(users(interview.getCreatedBy()), authorId, "FEEDBACK_SUBMITTED",
                    "%s a laissé un feedback pour l'entretien avec %s (« %s »)".formatted(
                            fullName(author),
                            fullName(interview.getApplication().getCandidate()),
                            interview.getApplication().getJobOffer().getTitle()),
                    "INTERVIEW", interviewId);
        });
    }

    /**
     * Envoie les rappels des entretiens qui commencent dans les 24 h. Chaque rappel est réservé
     * atomiquement en base : même avec plusieurs instances, il n'est envoyé qu'une fois.
     */
    @Transactional
    public int sendDueReminders(Instant now) {
        int sent = 0;
        for (Interview interview : interviewRepository.findDueForReminder(now, now.plus(REMINDER_HORIZON))) {
            if (interviewRepository.claimReminder(interview.getId(), now) == 0) {
                continue;
            }
            emailInterview(interview, EmailTemplate.INTERVIEW_REMINDER);
            notifyStaff(interview.getParticipants(), null, "INTERVIEW_REMINDER",
                    "Rappel : entretien avec %s le %s".formatted(
                            fullName(interview.getApplication().getCandidate()),
                            formatWhen(interview.getStartAt())),
                    "INTERVIEW", interview.getId());
            sent++;
        }
        return sent;
    }

    // ---------- helpers ----------

    private void emailInterview(Interview interview, EmailTemplate template) {
        User candidate = interview.getApplication().getCandidate();
        String jobTitle = interview.getApplication().getJobOffer().getTitle();
        email(candidate, template, jobTitle, interviewVars(interview, fullName(candidate), true));
        for (User participant : interview.getParticipants()) {
            email(participant, template, jobTitle, interviewVars(interview, fullName(participant), false));
        }
    }

    private Map<String, Object> interviewVars(Interview interview, String recipientName, boolean forCandidate) {
        Application application = interview.getApplication();
        String location = interview.getLocationOrLink();
        return vars(
                "recipientName", recipientName,
                "audienceCandidate", forCandidate,
                "jobTitle", application.getJobOffer().getTitle(),
                "candidateName", fullName(application.getCandidate()),
                "when", formatWhen(interview.getStartAt()),
                "durationMin", interview.getDurationMin(),
                "modeLabel", EmailLabels.mode(interview.getMode()),
                "location", location,
                "locationIsUrl", location != null
                        && (location.startsWith("https://") || location.startsWith("http://")));
    }

    private void email(User recipient, EmailTemplate template, String jobTitle, Map<String, Object> variables) {
        try {
            emailService.send(recipient.getEmail(), template.subject(jobTitle),
                    renderer.render(template, variables));
        } catch (RuntimeException ex) {
            // On ne journalise pas l'adresse : donnée personnelle.
            log.warn("Email {} could not be sent to user {}", template, recipient.getId(), ex);
        }
    }

    /** Notifications in-app : réservées au staff, sans doublon, sans l'auteur de l'action. */
    private void notifyStaff(Collection<User> recipients, Long excludeUserId, String type, String message,
                             String referenceType, Long referenceId) {
        List<Long> done = new ArrayList<>();
        for (User user : recipients) {
            if (user == null || user.getRole() == Role.CANDIDATE || !user.isEnabled()
                    || Objects.equals(user.getId(), excludeUserId) || done.contains(user.getId())) {
                continue;
            }
            done.add(user.getId());
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType(type);
            notification.setMessage(message.length() > 500 ? message.substring(0, 497) + "..." : message);
            notification.setReferenceType(referenceType);
            notification.setReferenceId(referenceId);
            notificationRepository.save(notification);
        }
    }

    private String formatWhen(Instant instant) {
        ZoneId zone = ZoneId.of(mailProps.timezone());
        return DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH'h'mm", Locale.FRENCH)
                .withZone(zone).format(instant) + " (" + zone.getId() + ")";
    }

    private static List<User> users(User... users) {
        List<User> list = new ArrayList<>();
        for (User user : users) {
            if (user != null) {
                list.add(user);
            }
        }
        return list;
    }

    private static String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private static Map<String, Object> vars(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return new HashMap<>(map);
    }
}
