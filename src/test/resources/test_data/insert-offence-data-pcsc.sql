INSERT INTO offence
(code, description, revision_id, cjs_title, start_date, end_date, changed_date, created_date, last_updated_date, acts_and_sections)
VALUES('AF06999', 'Brought before the court as being absent without leave from the Armed Forces', 570173, 'Brought before the court as being absent without leave from the Armed Forces', '2009-11-02', NULL, '2020-01-16 15:19:02.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178', 'Contrary to section 19 of the Zoo Licensing Act 1981');
INSERT INTO offence
(code, description, revision_id, cjs_title, start_date, end_date, changed_date, created_date, last_updated_date, offence_type, max_period_is_life)
VALUES('AB14001', 'Fail to comply with an animal by-product requirement', 574415, 'Fail to comply with an animal by-product requirement', '2015-03-13', NULL, '2020-06-17 15:31:26.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178', 'CI', 'Y');
INSERT INTO offence
(code, description, revision_id, cjs_title, start_date, end_date, changed_date, created_date, last_updated_date)
VALUES('AB14002', 'Intentionally obstruct an authorised person', 574487, 'Intentionally obstruct an authorised person', '2015-03-13', NULL, '2020-06-17 15:31:26.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178');
INSERT INTO offence
(code, description, revision_id, cjs_title, start_date, end_date, changed_date, created_date, last_updated_date)
VALUES('AB14003', 'Fail to give to an authorised person information / assistance / provide facilities that person may require', 574449, 'CJS Title Fail to give to an authorised person information / assistance / provide facilities that person may require', '2015-03-13', NULL, '2020-06-17 15:31:26.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178');

INSERT INTO offence_schedule_mapping (offence_id, schedule_part_id) values ((select id from offence o where o.code = 'AB14001'), (select id from schedule_part where schedule_id = (select id from schedule where code = '15' and part_number = 1)));
INSERT INTO offence_schedule_mapping (offence_id, schedule_part_id) values ((select id from offence o where o.code = 'AB14001'), (select id from schedule_part where schedule_id = (select id from schedule where code = '15' and part_number = 2)));