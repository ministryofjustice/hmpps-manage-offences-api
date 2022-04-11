DELETE from offence;
DELETE from sdrs_load_result_history;

UPDATE sdrs_load_result SET status = NULL, load_type = NULL, load_date = NULL, last_successful_load_date = NULL;
