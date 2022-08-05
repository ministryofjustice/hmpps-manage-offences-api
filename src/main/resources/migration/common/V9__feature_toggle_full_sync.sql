UPDATE feature_toggle SET feature = 'DELTA_SYNC_SDRS' WHERE feature = 'SYNC_SDRS';
INSERT INTO feature_toggle (feature, enabled) VALUES ('FULL_SYNC_SDRS', false);
