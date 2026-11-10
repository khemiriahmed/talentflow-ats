package com.talentflow;

import com.talentflow.notification.EmailService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** EmailService de test : mémorise les emails au lieu de les envoyer. */
public class RecordingEmailService implements EmailService {

    public record SentEmail(String to, String subject, String html) {
    }

    private final List<SentEmail> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(String to, String subject, String htmlBody) {
        sent.add(new SentEmail(to, subject, htmlBody));
    }

    public List<SentEmail> to(String email) {
        return sent.stream().filter(m -> m.to().equals(email)).toList();
    }
}
