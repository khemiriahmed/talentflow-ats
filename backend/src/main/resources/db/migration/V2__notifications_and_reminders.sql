-- Lien profond des notifications in-app (ex. APPLICATION #12, INTERVIEW #7)
ALTER TABLE notifications
    ADD COLUMN reference_type VARCHAR(30),
    ADD COLUMN reference_id   BIGINT;

-- Rappel d'entretien : horodatage d'envoi (évite les doublons)
ALTER TABLE interviews
    ADD COLUMN reminder_sent_at TIMESTAMPTZ;

CREATE INDEX idx_interviews_pending_reminder ON interviews (start_at)
    WHERE status = 'SCHEDULED' AND reminder_sent_at IS NULL;
