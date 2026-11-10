package com.talentflow.application;

import com.talentflow.audit.AuditService;
import com.talentflow.auth.CustomUserDetails;
import com.talentflow.common.BadRequestException;
import com.talentflow.notification.DomainEvents;
import com.talentflow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Pipeline kanban : RECEIVED, SCREENING, HR_INTERVIEW, TECH_INTERVIEW, OFFER, HIRED / REJECTED.
 * Déplacement libre entre étapes actives (drag & drop), avec quelques règles métier.
 */
@Service
@RequiredArgsConstructor
public class PipelineService {

    private final ApplicationAccess access;
    private final ApplicationRepository applicationRepository;
    private final StageHistoryRepository stageHistoryRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ApplicationMapper applicationMapper;
    private final PipelineMapper pipelineMapper;
    private final ApplicationEventPublisher events;

    @Transactional
    public ApplicationDto changeStage(Long applicationId, Stage target, String reason, CustomUserDetails actor) {
        Application application = access.findVisible(applicationId, actor);
        Stage current = application.getCurrentStage();
        validateTransition(current, target, reason);

        application.setCurrentStage(target);

        StageHistory history = new StageHistory();
        history.setApplication(application);
        history.setFromStage(current);
        history.setToStage(target);
        history.setReason(reason == null || reason.isBlank() ? null : reason.trim());
        history.setChangedBy(userRepository.getReferenceById(actor.getId()));
        stageHistoryRepository.save(history);

        auditService.log(actor.getId(), "APPLICATION_STAGE_" + target, "Application", applicationId);
        events.publishEvent(new DomainEvents.StageChanged(applicationId, current, target, actor.getId()));
        return applicationMapper.toDto(application);
    }

    @Transactional(readOnly = true)
    public List<StageHistoryDto> history(Long applicationId, CustomUserDetails user) {
        access.findVisible(applicationId, user);
        return stageHistoryRepository.findByApplicationIdOrderByChangedAtDescIdDesc(applicationId)
                .stream().map(pipelineMapper::toDto).toList();
    }

    private static void validateTransition(Stage current, Stage target, String reason) {
        if (current == Stage.HIRED) {
            throw new BadRequestException("A hired application cannot be moved");
        }
        if (target == current) {
            throw new BadRequestException("The application is already in this stage");
        }
        if (target == Stage.REJECTED && (reason == null || reason.isBlank())) {
            throw new BadRequestException("A reason is required to reject an application");
        }
        if (target == Stage.HIRED && current != Stage.OFFER) {
            throw new BadRequestException("Only an application in the OFFER stage can be marked as hired");
        }
    }
}
