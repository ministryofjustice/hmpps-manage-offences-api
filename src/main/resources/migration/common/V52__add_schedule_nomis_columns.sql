ALTER TABLE offence_to_sync_with_nomis ADD COLUMN nomis_schedule_name VARCHAR(1024);

INSERT INTO feature_toggle (feature, enabled) VALUES ('SYNCHRONISE_SCHEDULE_MAPPINGS_NOMIS', false);
