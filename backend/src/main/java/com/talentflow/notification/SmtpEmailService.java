package com.talentflow.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final AppMailProperties props;

    @Override
    public void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(props.from());
            helper.setTo(to);
            helper.setSubject(subject.replaceAll("[\\r\\n]+", " "));   // anti injection d'en-têtes
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException | MailException ex) {
            throw new EmailException("Failed to send email", ex);
        }
    }
}
