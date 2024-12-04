INSERT INTO schedule (act, code, url)
VALUES ('Tranche Three Murder Excluded Offences', 'MURD', null);

INSERT INTO schedule_part (schedule_id, part_number)
VALUES ((SELECT id FROM schedule WHERE code = 'SEO'), 2),
       ((SELECT id FROM schedule WHERE code = 'DVEO'), 2),
       ((SELECT id FROM schedule WHERE code = 'MURD'), 1);

INSERT INTO offence_schedule_mapping (schedule_part_id, legislation_text, offence_id)
VALUES
    ((SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'MURD') AND part_number = 1), 'Contrary to Common Law', (SELECT id FROM offence WHERE code = 'COML025')),
    ((SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'MURD') AND part_number = 1), 'Contrary to Common Law', (SELECT id FROM offence WHERE code = 'COML026'));