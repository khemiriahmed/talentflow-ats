package com.talentflow.application;

import com.talentflow.auth.CustomUserDetails;
import com.talentflow.common.BadRequestException;
import com.talentflow.common.ConflictException;
import com.talentflow.common.PageRequests;
import com.talentflow.common.ResourceNotFoundException;
import com.talentflow.job.JobOffer;
import com.talentflow.job.JobOfferRepository;
import com.talentflow.job.JobStatus;
import com.talentflow.storage.StorageService;
import com.talentflow.user.Role;
import com.talentflow.user.User;
import com.talentflow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private static final long MAX_CV_BYTES = 5L * 1024 * 1024;
    private static final int MAX_COVER_LETTER_CHARS = 5000;
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};

    private final ApplicationRepository applicationRepository;
    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;
    private final StageHistoryRepository stageHistoryRepository;
    private final StorageService storageService;
    private final ApplicationMapper mapper;

    // ---------- candidat ----------

    @Transactional
    public ApplicationDto apply(Long jobOfferId, MultipartFile cv, String coverLetter,
                                String source, Long candidateId) {
        JobOffer job = jobOfferRepository.findById(jobOfferId)
                .filter(j -> j.getStatus() == JobStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Job offer not found"));
        if (job.getDeadline() != null && job.getDeadline().isBefore(LocalDate.now())) {
            throw new BadRequestException("Applications for this job offer are closed");
        }
        if (applicationRepository.existsByJobOfferIdAndCandidateId(jobOfferId, candidateId)) {
            throw new ConflictException("You have already applied to this job offer");
        }
        if (coverLetter != null && coverLetter.length() > MAX_COVER_LETTER_CHARS) {
            throw new BadRequestException("Cover letter must not exceed " + MAX_COVER_LETTER_CHARS + " characters");
        }
        byte[] pdf = readPdf(cv);
        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String key = "cv/%d/%d-%s.pdf".formatted(jobOfferId, candidateId, UUID.randomUUID());

        Application application = new Application();
        application.setJobOffer(job);
        application.setCandidate(candidate);
        application.setCoverLetter(blankToNull(coverLetter));
        application.setSource(blankToNull(source));
        application.setCvFileKey(key);
        Application saved = applicationRepository.saveAndFlush(application);

        StageHistory history = new StageHistory();
        history.setApplication(saved);
        history.setToStage(Stage.RECEIVED);
        history.setChangedBy(candidate);
        stageHistoryRepository.save(history);

        // Upload en dernier : s'il échoue, la transaction est annulée et rien n'est enregistré.
        storageService.upload(key, pdf, "application/pdf");
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationDto> mine(Long candidateId, int page, int size) {
        return applicationRepository
                .findAll(ApplicationSpecs.ofCandidate(candidateId), PageRequests.newestFirst(page, size))
                .map(mapper::toDto);
    }

    /** Retrait d'une candidature (RF-23) : supprime la candidature, son historique et le CV. */
    @Transactional
    public void withdraw(Long applicationId, Long candidateId) {
        Application application = applicationRepository.findById(applicationId)
                .filter(a -> a.getCandidate().getId().equals(candidateId))
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (application.getCurrentStage() == Stage.HIRED) {
            throw new BadRequestException("A hired application cannot be withdrawn");
        }
        String key = application.getCvFileKey();
        applicationRepository.delete(application);

        // Le fichier n'est supprimé qu'après le commit de la base.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    storageService.delete(key);
                } catch (RuntimeException ex) {
                    log.warn("Could not delete CV {} after withdrawal", key, ex);
                }
            }
        });
    }

    // ---------- staff + candidat ----------

    @Transactional(readOnly = true)
    public Page<ApplicationDto> list(CustomUserDetails user, Long jobOfferId, Stage stage,
                                     String q, int page, int size) {
        Specification<Application> spec = Specification
                .where(ApplicationSpecs.forJobOffer(jobOfferId))
                .and(ApplicationSpecs.inStage(stage))
                .and(ApplicationSpecs.candidateMatches(q));
        if (user.getRole() == Role.MANAGER) {
            spec = spec.and(ApplicationSpecs.managedBy(user.getId()));
        }
        return applicationRepository.findAll(spec, PageRequests.newestFirst(page, size))
                .map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public ApplicationDto get(Long id, CustomUserDetails user) {
        return mapper.toDto(findVisible(id, user));
    }

    @Transactional(readOnly = true)
    public CvFile getCv(Long id, CustomUserDetails user) {
        Application application = findVisible(id, user);
        return new CvFile(storageService.download(application.getCvFileKey()), "cv-" + id + ".pdf");
    }

    // ---------- helpers ----------

    /** Un utilisateur sans accès reçoit un 404 (on ne révèle pas l'existence de la candidature). */
    private Application findVisible(Long id, CustomUserDetails user) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        boolean allowed = switch (user.getRole()) {
            case ADMIN, HR -> true;
            case MANAGER -> application.getJobOffer().getManager() != null
                    && application.getJobOffer().getManager().getId().equals(user.getId());
            case CANDIDATE -> application.getCandidate().getId().equals(user.getId());
        };
        if (!allowed) {
            throw new ResourceNotFoundException("Application not found");
        }
        return application;
    }

    private byte[] readPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("A CV file is required");
        }
        if (file.getSize() > MAX_CV_BYTES) {
            throw new BadRequestException("The CV must not exceed 5 MB");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("The CV could not be read");
        }
        // On vérifie la signature du fichier, pas le Content-Type envoyé par le client.
        if (bytes.length < PDF_MAGIC.length) {
            throw new BadRequestException("The CV must be a PDF file");
        }
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (bytes[i] != PDF_MAGIC[i]) {
                throw new BadRequestException("The CV must be a PDF file");
            }
        }
        return bytes;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
