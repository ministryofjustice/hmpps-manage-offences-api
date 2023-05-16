ALTER TABLE offence_reactivated_in_nomis ADD COLUMN offence_code VARCHAR (30);
ALTER TABLE offence_reactivated_in_nomis ADD CONSTRAINT unique_offence_reactivated_code UNIQUE (offence_code);
UPDATE offence_reactivated_in_nomis orin SET offence_code = (SELECT code FROM offence o WHERE o.id = orin.offence_id);
ALTER TABLE offence_reactivated_in_nomis ALTER COLUMN offence_code SET NOT NULL;