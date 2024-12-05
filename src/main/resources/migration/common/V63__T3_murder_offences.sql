INSERT INTO schedule (act, code, url)
VALUES ('Tranche Three Murder Excluded Offences', 'MURD', null);

INSERT INTO schedule_part (schedule_id, part_number)
VALUES ((SELECT id FROM schedule WHERE code = 'SEO'), 2),
       ((SELECT id FROM schedule WHERE code = 'DVEO'), 2),
       ((SELECT id FROM schedule WHERE code = 'MURD'), 1);

-- Dropping the constraints for the integration tests, are re-enabled after the inserts (this is not necessary for the real environments as all the expected offences exist)
ALTER TABLE offence_schedule_mapping DROP CONSTRAINT fk_schedule_to_offence_constraint;
ALTER TABLE offence_schedule_mapping ALTER COLUMN offence_id DROP NOT NULL;

INSERT INTO offence_schedule_mapping (schedule_part_id, legislation_text, offence_id)
VALUES
    ((SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'MURD') AND part_number = 1), 'Contrary to Common Law', (SELECT id FROM offence WHERE code = 'COML025')),
    ((SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'MURD') AND part_number = 1), 'Contrary to Common Law', (SELECT id FROM offence WHERE code = 'COML026')),
    ((SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'SEO') AND part_number = 2), 'Contrary to section 33(1) and (9) of the Criminal Justice and Courts Act 2015.', (SELECT id FROM offence WHERE code = 'CJ15005')),
    ((SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'SEO') AND part_number = 2), 'Contrary to section 33(1) and (9) of the Criminal Justice and Courts Act 2015.', (SELECT id FROM offence WHERE code = 'CJ15013')),
    ((SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'SEO') AND part_number = 2), 'Contrary to section 354(1) and (4) of the Sentencing Act 2020.', (SELECT id FROM offence WHERE code = 'SE20005')),
    ((SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'SEO') AND part_number = 2), 'Contrary to section 1(1) of the Criminal Attempts Act 1981 and section 354(1) and (4) of the Sentencing Act 2020.', (SELECT id FROM offence WHERE code = 'SE20006')),
    ((SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'SEO') AND part_number = 2), 'Contrary to section 354(1) and (4) of the Sentencing Act 2020.', (SELECT id FROM offence WHERE code = 'SE20012')),
    ((SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'SEO') AND part_number = 2), 'Contrary to section 354(1)(a) and (4) of the Sentencing Act 2020 and section 1(1) of the Criminal Attempts Act 1981.', (SELECT id FROM offence WHERE code = 'SE20012A')),
    ((SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'DVEO') AND part_number = 2), 'Contrary to section 8(1) and (2) of the Stalking Protection Act 2019.', (SELECT id FROM offence WHERE code = 'ST19001')),
    ((SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'DVEO') AND part_number = 2), 'Contrary to section 5(5) and (6) of the Protection from Harassment Act 1997.', (SELECT id FROM offence WHERE code = 'PH97003')),
    ((SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'DVEO') AND part_number = 2), 'Contrary to section 5(5) and (6) of the Protection from Harassment Act 1997.', (SELECT id FROM offence WHERE code = 'PH97005')),
    ((SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'DVEO') AND part_number = 2), 'Contrary to section 5(5) and (6) of the Protection from Harassment Act 1997 and section 1(1) of the Criminal Attempts Act 1981.', (SELECT id FROM offence WHERE code = 'PH97003A')),
    ((SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'DVEO') AND part_number = 2), 'Contrary to section 5(5) and (6) of the Protection from Harassment Act 1997.', (SELECT id FROM offence WHERE code = 'PH97003B')),
    ((SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'DVEO') AND part_number = 2), 'Contrary to section 5A(2D) and (2E) of the Protection from Harassment Act 1997.', (SELECT id FROM offence WHERE code = 'PH97012'));

DELETE FROM offence_schedule_mapping WHERE offence_id IS NULL;
ALTER TABLE offence_schedule_mapping ADD CONSTRAINT fk_schedule_to_offence_constraint FOREIGN KEY (offence_id) REFERENCES offence(id);
ALTER TABLE offence_schedule_mapping ALTER COLUMN offence_id SET NOT NULL;