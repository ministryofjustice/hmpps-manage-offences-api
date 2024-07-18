INSERT INTO schedule (act, code)
VALUES ('Sexual Excluded Offences', 'SEO');

INSERT INTO schedule_part (schedule_id, part_number) VALUES ((SELECT id FROM schedule WHERE code = 'SEO'), 1);

-- Dropping the constraints for the integration tests, are re-enabled after the inserts (this is not necessary for the real environments as all the expected offences exist)
ALTER TABLE offence_schedule_mapping DROP CONSTRAINT fk_schedule_to_offence_constraint;
ALTER TABLE offence_schedule_mapping ALTER COLUMN offence_id DROP NOT NULL;

INSERT INTO offence_schedule_mapping (schedule_part_id, legislation_text, offence_id) VALUES ((SELECT id FROM schedule_part WHERE schedule_id =  (SELECT id FROM schedule WHERE code = 'SEO') and part_number = 1),(SELECT acts_and_sections FROM offence WHERE code = 'SA00001'),(SELECT id FROM offence WHERE code = 'SA00001'));
INSERT INTO offence_schedule_mapping (schedule_part_id, legislation_text, offence_id) VALUES ((SELECT id FROM schedule_part WHERE schedule_id =  (SELECT id FROM schedule WHERE code = 'SEO') and part_number = 1),(SELECT acts_and_sections FROM offence WHERE code = 'SA00002'),(SELECT id FROM offence WHERE code = 'SA00002'));
DELETE FROM offence_schedule_mapping WHERE offence_id IS NULL;

ALTER TABLE offence_schedule_mapping ADD CONSTRAINT fk_schedule_to_offence_constraint FOREIGN KEY (offence_id) REFERENCES offence(id);
ALTER TABLE offence_schedule_mapping ALTER COLUMN offence_id SET NOT NULL;
