INSERT INTO offence
(code, description, revision_id, cjs_title, start_date, end_date, changed_date, created_date, last_updated_date)
VALUES
('PM01', 'Some exclusion', 570173, 'Some exclusion CJS', '2009-11-02', NULL, '2020-01-16 15:19:02.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178'),
('PM02', 'Another exclusion', 574415, 'Another exclusion CJS', '2015-03-13', NULL, '2020-06-17 15:31:26.000', '2022-04-07 16:05:58.178', '2022-04-07');

-- actual schedule created in the UI. this is only for the tests
INSERT INTO schedule (act, code, url, status)
VALUES ('SA2026 Excluded Offences for Progression Model', 'SA2026 EOPM', '/test', 'LIVE');
INSERT INTO schedule_part (schedule_id, part_number)
VALUES ((SELECT id FROM schedule WHERE code = 'SA2026 EOPM'), 1);
INSERT INTO offence_schedule_mapping (offence_id, schedule_part_id) values ((select id from offence o where o.code = 'PM01'), (select id from schedule_part where schedule_id = (select id from schedule where code = 'SA2026 EOPM' and part_number = 1)));
INSERT INTO offence_schedule_mapping (offence_id, schedule_part_id) values ((select id from offence o where o.code = 'PM02'), (select id from schedule_part where schedule_id = (select id from schedule where code = 'SA2026 EOPM' and part_number = 1)));
