ALTER TABLE feature_toggle ALTER COLUMN feature TYPE VARCHAR(64);
INSERT INTO feature_toggle (feature, enabled) VALUES ('SEXUAL_OFFENCES_FROM_CODES_AND_S15P2', true);