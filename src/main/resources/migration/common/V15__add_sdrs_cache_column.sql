ALTER TABLE sdrs_load_result ADD COLUMN cache varchar(50);
ALTER TABLE sdrs_load_result_history ADD COLUMN cache varchar(50);
ALTER TABLE offence ADD COLUMN sdrs_cache varchar(50);
ALTER TABLE offence ALTER COLUMN changed_date SET NOT NULL;
ALTER TABLE offence ALTER COLUMN start_date SET NOT NULL;
ALTER TABLE offence ALTER COLUMN revision_id SET NOT NULL;

UPDATE sdrs_load_result SET cache = 'OFFENCES_' || alpha_char;
UPDATE sdrs_load_result_history SET cache = 'OFFENCES_' || alpha_char;
UPDATE offence SET sdrs_cache = 'OFFENCES_' || substring(code, 1,1);

ALTER TABLE sdrs_load_result DROP COLUMN alpha_char;
ALTER TABLE sdrs_load_result_history DROP COLUMN alpha_char;
ALTER TABLE sdrs_load_result ALTER COLUMN cache SET NOT NULL;
ALTER TABLE sdrs_load_result ADD PRIMARY KEY (cache);

INSERT INTO sdrs_load_result (cache) VALUES ('GET_APPLICATIONS');
INSERT INTO sdrs_load_result (cache) VALUES ('GET_MOJ_OFFENCE');

ALTER TABLE offence ALTER COLUMN code SET NOT NULL;