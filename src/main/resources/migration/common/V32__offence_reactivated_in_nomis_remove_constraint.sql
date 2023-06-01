ALTER TABLE offence_reactivated_in_nomis DROP COLUMN offence_id;
ALTER TABLE offence_reactivated_in_nomis ADD PRIMARY KEY (offence_code);
