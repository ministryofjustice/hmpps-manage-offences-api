DELETE from offence_reactivated_in_nomis;
DELETE from offence_schedule_mapping;
DELETE from offence;
DELETE from sdrs_load_result_history;
DELETE from ho_codes_load_history;
DELETE from legacy_sdrs_ho_code_mapping;

UPDATE sdrs_load_result SET status = NULL, load_type = NULL, load_date = NULL, last_successful_load_date = NULL;
UPDATE feature_toggle set enabled = true;
UPDATE feature_toggle set enabled = false where feature = 'FULL_SYNC_SDRS';

