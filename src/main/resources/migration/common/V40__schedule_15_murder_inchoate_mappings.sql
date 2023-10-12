-- Dropping the constraints for the integration tests, are re-enabled after the inserts (this is not necessary for the real environments as all the expected offences exist)
ALTER TABLE offence_schedule_mapping DROP CONSTRAINT fk_schedule_to_offence_constraint;
ALTER TABLE offence_schedule_mapping ALTER COLUMN offence_id DROP NOT NULL;

-- Add Inchoate murder offences to Schedule 15 Part 1
INSERT INTO offence_schedule_mapping (schedule_part_id, legislation_text, offence_id, paragraph_number, paragraph_title) VALUES ((SELECT id FROM schedule_part WHERE schedule_id =  (SELECT id FROM schedule WHERE code = '15') and part_number = 1),'Contrary to Common Law.', (SELECT id FROM offence WHERE code = 'COML025A'), '65', 'Attempt/Conspire/Incite to Murder');
INSERT INTO offence_schedule_mapping (schedule_part_id, legislation_text, offence_id, paragraph_number, paragraph_title) VALUES ((SELECT id FROM schedule_part WHERE schedule_id =  (SELECT id FROM schedule WHERE code = '15') and part_number = 1),'Contrary to Common Law.', (SELECT id FROM offence WHERE code = 'COML025C'), '65', 'Attempt/Conspire/Incite to Murder');
INSERT INTO offence_schedule_mapping (schedule_part_id, legislation_text, offence_id, paragraph_number, paragraph_title) VALUES ((SELECT id FROM schedule_part WHERE schedule_id =  (SELECT id FROM schedule WHERE code = '15') and part_number = 1),'Contrary to Common Law.', (SELECT id FROM offence WHERE code = 'COML025I'), '65', 'Attempt/Conspire/Incite to Murder');
INSERT INTO offence_schedule_mapping (schedule_part_id, legislation_text, offence_id, paragraph_number, paragraph_title) VALUES ((SELECT id FROM schedule_part WHERE schedule_id =  (SELECT id FROM schedule WHERE code = '15') and part_number = 1),'Contrary to Common Law.', (SELECT id FROM offence WHERE code = 'COML026A'), '65', 'Attempt/Conspire/Incite to Murder');
INSERT INTO offence_schedule_mapping (schedule_part_id, legislation_text, offence_id, paragraph_number, paragraph_title) VALUES ((SELECT id FROM schedule_part WHERE schedule_id =  (SELECT id FROM schedule WHERE code = '15') and part_number = 1),'Contrary to Common Law.', (SELECT id FROM offence WHERE code = 'COML026C'), '65', 'Attempt/Conspire/Incite to Murder');
INSERT INTO offence_schedule_mapping (schedule_part_id, legislation_text, offence_id, paragraph_number, paragraph_title) VALUES ((SELECT id FROM schedule_part WHERE schedule_id =  (SELECT id FROM schedule WHERE code = '15') and part_number = 1),'Contrary to Common Law.', (SELECT id FROM offence WHERE code = 'COML026I'), '65', 'Attempt/Conspire/Incite to Murder');

DELETE FROM offence_schedule_mapping WHERE offence_id IS NULL;

ALTER TABLE offence_schedule_mapping ADD CONSTRAINT fk_schedule_to_offence_constraint FOREIGN KEY (offence_id) REFERENCES offence(id);
ALTER TABLE offence_schedule_mapping ALTER COLUMN offence_id SET NOT NULL;
