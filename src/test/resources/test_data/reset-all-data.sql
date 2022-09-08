DELETE from offence_schedule_part;
DELETE from offence;
DELETE from sdrs_load_result_history;

UPDATE sdrs_load_result SET status = NULL, load_type = NULL, load_date = NULL, last_successful_load_date = NULL;
UPDATE feature_toggle set enabled = true;
UPDATE feature_toggle set enabled = false where feature = 'FULL_SYNC_SDRS';

