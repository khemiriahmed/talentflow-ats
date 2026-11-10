package com.talentflow.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Rappels d'entretien 24 h avant (RF-50). Désactivable avec app.reminders.enabled=false. */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.reminders.enabled", havingValue = "true", matchIfMissing = true)
public class InterviewReminderJob {

    private final NotificationDispatcher dispatcher;

    @Scheduled(fixedDelayString = "${app.reminders.interval:PT15M}",
            initialDelayString = "${app.reminders.initial-delay:PT1M}")
    public void run() {
        try {
            int sent = dispatcher.sendDueReminders(Instant.now());
            if (sent > 0) {
                log.info("{} interview reminder(s) sent", sent);
            }
        } catch (RuntimeException ex) {
            log.error("Interview reminder job failed", ex);
        }
    }
}
