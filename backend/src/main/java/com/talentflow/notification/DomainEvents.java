package com.talentflow.notification;

import com.talentflow.application.Stage;

/** Événements métier publiés dans les transactions ; traités après commit, en asynchrone. */
public final class DomainEvents {

    private DomainEvents() {
    }

    public record ApplicationSubmitted(Long applicationId) {
    }

    public record StageChanged(Long applicationId, Stage from, Stage to, Long actorId) {
    }

    public record InterviewScheduled(Long interviewId, Long actorId) {
    }

    public record InterviewCancelled(Long interviewId, Long actorId) {
    }

    public record FeedbackSubmitted(Long interviewId, Long authorId) {
    }

    /** Le jeton brut n'est jamais persisté : il n'existe que le temps de l'envoi de l'email. */
    public record EmailVerificationRequested(Long userId, String token) {
    }

    public record PasswordResetRequested(Long userId, String token) {
    }
}
