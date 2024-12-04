INSERT INTO schedule (act, code, url) VALUES
       ('Tranche Three Murder Excluded Offences', 'MURD', null);

INSERT INTO schedule_part (schedule_id, part_number)
VALUES ((SELECT id FROM schedule WHERE code = 'SEO'), 2),
       ((SELECT id FROM schedule WHERE code = 'DVEO'), 2),
       ((SELECT id FROM schedule WHERE code = 'MURD'), 1);