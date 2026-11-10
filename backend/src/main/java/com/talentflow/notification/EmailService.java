package com.talentflow.notification;

/** Envoi d'un email HTML. Implémentation SMTP (MailHog en local, AWS SES ou autre en production). */
public interface EmailService {

    void send(String to, String subject, String htmlBody);
}
