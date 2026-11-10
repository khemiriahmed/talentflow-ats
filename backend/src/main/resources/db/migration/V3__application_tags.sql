-- Tags libres sur les candidatures (ex. "java", "spring boot", "à rappeler")
CREATE TABLE application_tags (
    application_id  BIGINT      NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    tag             VARCHAR(50) NOT NULL,
    PRIMARY KEY (application_id, tag)
);
CREATE INDEX idx_application_tags_tag ON application_tags (tag);
