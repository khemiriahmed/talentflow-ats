package com.talentflow.notification;

/** Modèles d'emails : les fichiers HTML sont dans src/main/resources/templates/email/. */
public enum EmailTemplate {

    APPLICATION_RECEIVED("email/application-received", "Votre candidature a bien été reçue"),
    STAGE_CHANGED("email/stage-changed", "Mise à jour de votre candidature"),
    INTERVIEW_INVITATION("email/interview-invitation", "Invitation à un entretien"),
    INTERVIEW_CANCELLED("email/interview-cancelled", "Entretien annulé"),
    INTERVIEW_REMINDER("email/interview-reminder", "Rappel de votre entretien");

    private final String templateName;
    private final String subject;

    EmailTemplate(String templateName, String subject) {
        this.templateName = templateName;
        this.subject = subject;
    }

    public String templateName() {
        return templateName;
    }

    public String subject(String jobTitle) {
        return subject + " - " + jobTitle;
    }
}
