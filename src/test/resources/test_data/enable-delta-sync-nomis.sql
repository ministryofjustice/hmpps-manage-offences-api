UPDATE feature_toggle SET enabled = FALSE;
UPDATE feature_toggle SET enabled = TRUE WHERE feature = 'DELTA_SYNC_NOMIS';
