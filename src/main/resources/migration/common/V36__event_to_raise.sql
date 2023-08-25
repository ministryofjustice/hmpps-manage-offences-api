CREATE TABLE event_to_raise
(
    id           INT GENERATED ALWAYS AS IDENTITY,
    event_type   VARCHAR(50) NOT NULL,
    offence_code VARCHAR(30) NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO feature_toggle (feature, enabled) VALUES ('PUBLISH_EVENTS', false);
