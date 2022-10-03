UPDATE feature_toggle set enabled = false;
UPDATE feature_toggle set enabled = true where feature = 'DELTA_SYNC_SDRS';
UPDATE feature_toggle set enabled = true where feature = 'DELTA_SYNC_NOMIS';
