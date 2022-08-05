UPDATE feature_toggle set enabled = true where feature = 'FULL_SYNC_SDRS';
UPDATE feature_toggle set enabled = false where feature = 'DELTA_SYNC_SDRS';
