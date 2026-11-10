package com.talentflow.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Réagit aux événements métier : après le commit de la transaction d'origine, dans un pool dédié.
 * Un problème de notification ne peut donc jamais faire échouer l'action métier.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationDispatcher dispatcher;

    @Async("notificationExecutor")
    @TransactionalEventListener
    public void applicationSubmitted(DomainEvents.ApplicationSubmitted event) {
        run("applicationSubmitted", () -> dispatcher.onApplicationSubmitted(event.applicationId()));
    }

    @Async("notificationExecutor")
    @TransactionalEventListener
    public void stageChanged(DomainEvents.StageChanged event) {
        run("stageChanged", () -> dispatcher.onStageChanged(event.applicationId(), event.to(), event.actorId()));
    }

    @Async("notificationExecutor")
    @TransactionalEventListener
    public void interviewScheduled(DomainEvents.InterviewScheduled event) {
        run("interviewScheduled", () -> dispatcher.onInterviewScheduled(event.interviewId(), event.actorId()));
    }

    @Async("notificationExecutor")
    @TransactionalEventListener
    public void interviewCancelled(DomainEvents.InterviewCancelled event) {
        run("interviewCancelled", () -> dispatcher.onInterviewCancelled(event.interviewId(), event.actorId()));
    }

    @Async("notificationExecutor")
    @TransactionalEventListener
    public void feedbackSubmitted(DomainEvents.FeedbackSubmitted event) {
        run("feedbackSubmitted", () -> dispatcher.onFeedbackSubmitted(event.interviewId(), event.authorId()));
    }

    private void run(String name, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            log.error("Notification handler {} failed", name, ex);
        }
    }
}
