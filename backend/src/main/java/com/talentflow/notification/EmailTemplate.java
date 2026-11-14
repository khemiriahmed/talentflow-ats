package com.talentflow.notification;

/** Modèles d'emails : les fichiers HTML sont dans src/main/resources/templates/email/. */
public enum EmailTemplate {

    APPLICATION_RECEIVED("email/application-received", "Votre candidature a bien été reçue"),
    STAGE_CHANGED("email/stage-changed", "Mise à jour de votre candidature"),
    INTERVIEW_INVITATION("email/interview-invitation", "Invitation à un entretien"),
    INTERVIEW_CANCELLED("email/interview-cancelled", "Entretien annulé"),
    INTERVIEW_REMINDER("email/interview-reminder", "Rappel de votre entretien"),
    VERIFY_EMAIL("email/verify-email", "Vérifiez votre adresse e-mail"),
    PASSWORD_RESET("email/password-reset", "Réinitialisation de votre mot de passe");

    private final String templateName;
    private final String subject;

    EmailTemplate(String templateName, String subject) {
        this.templateName = templateName;
        this.subject = subject;
    }

    public String templateName() {
        return templateName;
    }

    public String subject() {
        return subject;
    }

    public String subject(String jobTitle) {
        return subject + " - " + jobTitle;
    }
}
