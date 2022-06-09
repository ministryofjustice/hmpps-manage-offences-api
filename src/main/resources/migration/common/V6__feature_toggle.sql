CREATE TABLE IF NOT EXISTS feature_toggle (
    feature VARCHAR(32),
    enabled BOOLEAN,
    PRIMARY KEY (feature)
);

INSERT INTO feature_toggle (feature, enabled) VALUES ('FULL_SYNC_NOMIS', false);
INSERT INTO feature_toggle (feature, enabled) VALUES ('DELTA_SYNC_NOMIS', false);
INSERT INTO feature_toggle (feature, enabled) VALUES ('SYNC_SDRS', false);
