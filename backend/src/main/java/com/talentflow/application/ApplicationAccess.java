package com.talentflow.application;

import com.talentflow.auth.CustomUserDetails;
import com.talentflow.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Règle d'accès unique à une candidature, partagée par les candidatures, le pipeline,
 * les notes et les entretiens. À appeler dans une transaction.
 */
@Component
@RequiredArgsConstructor
public class ApplicationAccess {

    private final ApplicationRepository applicationRepository;

    /** Un utilisateur sans accès reçoit un 404 (on ne révèle pas l'existence de la candidature). */
    public Application findVisible(Long id, CustomUserDetails user) {
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
}
