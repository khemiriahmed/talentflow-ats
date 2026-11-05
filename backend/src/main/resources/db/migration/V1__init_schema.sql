-- Utilisateurs
CREATE TABLE users (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(30),
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('CANDIDATE', 'HR', 'MANAGER', 'ADMIN')),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Offres d'emploi
CREATE TABLE job_offers (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title          VARCHAR(200) NOT NULL,
    description    TEXT         NOT NULL,
    department     VARCHAR(100),
    location       VARCHAR(100),
    contract_type  VARCHAR(30)  NOT NULL,
    salary_min     NUMERIC(12, 2),
    salary_max     NUMERIC(12, 2),
    status         VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                   CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED', 'ARCHIVED')),
    deadline       DATE,
    created_by     BIGINT       NOT NULL REFERENCES users (id),
    manager_id     BIGINT       REFERENCES users (id),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_job_offers_status ON job_offers (status);

-- Candidatures
CREATE TABLE applications (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    job_offer_id   BIGINT       NOT NULL REFERENCES job_offers (id),
    candidate_id   BIGINT       NOT NULL REFERENCES users (id),
    cover_letter   TEXT,
    cv_file_key    VARCHAR(500) NOT NULL,
    current_stage  VARCHAR(30)  NOT NULL DEFAULT 'RECEIVED'
                   CHECK (current_stage IN ('RECEIVED', 'SCREENING', 'HR_INTERVIEW',
                                            'TECH_INTERVIEW', 'OFFER', 'HIRED', 'REJECTED')),
    source         VARCHAR(50),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (job_offer_id, candidate_id)
);
CREATE INDEX idx_applications_stage ON applications (current_stage);
CREATE INDEX idx_applications_candidate ON applications (candidate_id);

-- Historique des étapes
CREATE TABLE stage_history (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id  BIGINT      NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    from_stage      VARCHAR(30),
    to_stage        VARCHAR(30) NOT NULL,
    reason          TEXT,
    changed_by      BIGINT      NOT NULL REFERENCES users (id),
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Notes internes
CREATE TABLE notes (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id  BIGINT      NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    author_id       BIGINT      NOT NULL REFERENCES users (id),
    content         TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Entretiens
CREATE TABLE interviews (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id    BIGINT       NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    start_at          TIMESTAMPTZ  NOT NULL,
    duration_min      INTEGER      NOT NULL DEFAULT 60,
    mode              VARCHAR(20)  NOT NULL CHECK (mode IN ('ONLINE', 'ON_SITE')),
    location_or_link  VARCHAR(500),
    status            VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED'
                      CHECK (status IN ('SCHEDULED', 'DONE', 'CANCELLED')),
    created_by        BIGINT       NOT NULL REFERENCES users (id),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_interviews_start ON interviews (start_at);

CREATE TABLE interview_participants (
    interview_id  BIGINT NOT NULL REFERENCES interviews (id) ON DELETE CASCADE,
    user_id       BIGINT NOT NULL REFERENCES users (id),
    PRIMARY KEY (interview_id, user_id)
);

-- Feedbacks d'entretien
CREATE TABLE feedbacks (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    interview_id    BIGINT      NOT NULL UNIQUE REFERENCES interviews (id) ON DELETE CASCADE,
    author_id       BIGINT      NOT NULL REFERENCES users (id),
    rating          SMALLINT    NOT NULL CHECK (rating BETWEEN 1 AND 5),
    strengths       TEXT,
    weaknesses      TEXT,
    recommendation  VARCHAR(20) NOT NULL CHECK (recommendation IN ('STRONG_YES', 'YES', 'NO', 'STRONG_NO')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Notifications in-app
CREATE TABLE notifications (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type        VARCHAR(50)  NOT NULL,
    message     VARCHAR(500) NOT NULL,
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id, is_read);

-- Journal d'audit
CREATE TABLE audit_logs (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id     BIGINT REFERENCES users (id),
    action       VARCHAR(100) NOT NULL,
    entity_type  VARCHAR(50),
    entity_id    BIGINT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
