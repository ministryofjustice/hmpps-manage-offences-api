DELETE FROM offence_reactivated_in_nomis;
DELETE FROM offence_schedule_mapping;
DELETE FROM offence;
DELETE FROM sdrs_load_result_history;
DELETE FROM ho_codes_load_history;
DELETE FROM legacy_sdrs_ho_code_mapping;
DELETE FROM offence_to_sync_with_nomis;
DELETE FROM previous_offence_to_ho_code_mapping;
DELETE FROM home_office_code;

UPDATE sdrs_load_result SET status = NULL, load_type = NULL, load_date = NULL, last_successful_load_date = NULL;
UPDATE feature_toggle SET enabled = TRUE;
UPDATE feature_toggle SET enabled = FALSE WHERE feature = 'FULL_SYNC_SDRS';
UPDATE feature_toggle SET enabled = TRUE WHERE feature = 'SEXUAL_OFFENCES_FROM_CODES_AND_S15P2';

