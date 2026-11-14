-- Profil candidat / utilisateur (RF-04)
ALTER TABLE users
    ADD COLUMN linkedin_url VARCHAR(500),
    ADD COLUMN photo_url    VARCHAR(500);

-- Vérification d'email et réinitialisation du mot de passe (RF-01, RF-03).
-- Seul le condensat SHA-256 du jeton est stocké : une fuite de la base ne permet pas
-- de réutiliser les liens envoyés par email.
ALTER TABLE users
    ADD COLUMN email_verification_token_hash VARCHAR(64),
    ADD COLUMN email_verification_expires_at TIMESTAMPTZ,
    ADD COLUMN password_reset_token_hash      VARCHAR(64),
    ADD COLUMN password_reset_expires_at      TIMESTAMPTZ;

CREATE INDEX idx_users_email_verification_token ON users (email_verification_token_hash)
    WHERE email_verification_token_hash IS NOT NULL;
CREATE INDEX idx_users_password_reset_token ON users (password_reset_token_hash)
    WHERE password_reset_token_hash IS NOT NULL;
