INSERT INTO schedule_part (schedule_id, part_number) (SELECT id, 3 FROM schedule WHERE code = '13');
INSERT INTO offence_schedule_mapping (offence_id, schedule_part_id) values ((select id from offence o where o.code = 'NSLEGIS'), (select id from schedule_part where schedule_id = (select id from schedule where code = '13') AND part_number = 3));
UPDATE offence SET max_period_is_life = true WHERE code = 'AB14002';
