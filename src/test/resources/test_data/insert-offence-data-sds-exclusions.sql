INSERT INTO offence
(code, description, revision_id, cjs_title, start_date, end_date, changed_date, created_date, last_updated_date, acts_and_sections)
VALUES
('AF06999', 'Brought before the court as being absent without leave from the Armed Forces', 570173, 'Brought before the court as being absent without leave from the Armed Forces', '2009-11-02', NULL, '2020-01-16 15:19:02.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178', 'Contrary to section 19 of the Zoo Licensing Act 1981'),
('AB14001', 'Fail to comply with an animal by-product requirement', 574415, 'Fail to comply with an animal by-product requirement', '2015-03-13', NULL, '2020-06-17 15:31:26.000', '2022-04-07 16:05:58.178', '2022-04-07', null),
('AB14002', 'Intentionally obstruct an authorised person', 574487, 'Intentionally obstruct an authorised person', '2015-03-13', NULL, '2020-06-17 15:31:26.000', '2022-04-07 16:05:58.178', '2022-04-07', null),
('AB14003', 'Fail to give to an authorised person information / assistance / provide facilities that person may require', 574449, 'CJS Title Fail to give to an authorised person information / assistance / provide facilities that person may require', '2015-03-13', NULL, '2020-06-17 15:31:26.000', '2022-04-07 16:05:58.178', '2022-04-07', null),
('SX03TEST', 'Test for SX03 prefixed codes', 574450, 'Test for SX03 prefixed codes', '2015-03-13', NULL, '2020-06-17 15:31:26.000', '2022-04-07 16:05:58.178', '2022-04-07', null),
('SX56TEST', 'Test for SX56 prefixed codes', 574431, 'Test for SX56 prefixed codes', '2015-03-13', NULL, '2020-06-17 15:31:26.000', '2022-04-07 16:05:58.178', '2022-04-07', null),
('SXLEGIS', 'Test for sex legislation', 574432, 'Test for sex legislation', '2015-03-13', NULL, '2020-06-17 15:31:26.000', '2022-04-07 16:05:58.178', '2022-04-07', 'Sexual Offences Act 2003'),
('SA00001', 'Test for SA00001', 574432, 'Test for SA00001', '2015-03-13', NULL, '2020-06-17 15:31:26.000', '2022-04-07 16:05:58.178', '2022-04-07', 'Any act'),
('NSLEGIS', 'Test for NS legislation', 574432, 'Test for NS legislation', '2015-03-13', NULL, '2020-06-17 15:31:26.000', '2022-04-07 16:05:58.178', '2022-04-07', 'National Security Act 2023'),
('DV00001', 'Test for DV', 574432, 'Test for DV', '2015-03-13', NULL, '2020-06-17 15:31:26.000', '2022-04-07 16:05:58.178', '2022-04-07', 'xxx'),
('TR00001', 'Test for Terrorism', 574433, 'Test for Terrorism', '2015-03-13', NULL, '2020-06-17 15:31:26.000', '2022-04-07 16:05:58.178', '2022-04-07', 'xxx');

INSERT INTO offence_schedule_mapping (offence_id, schedule_part_id) values ((select id from offence o where o.code = 'AB14001'), (select id from schedule_part where schedule_id = (select id from schedule where code = '15' and part_number = 1)));
INSERT INTO offence_schedule_mapping (offence_id, schedule_part_id) values ((select id from offence o where o.code = 'AB14002'), (select id from schedule_part where schedule_id = (select id from schedule where code = '15' and part_number = 2)));
INSERT INTO offence_schedule_mapping (offence_id, schedule_part_id) values ((select id from offence o where o.code = 'AB14003'), (select id from schedule_part where schedule_id = (select id from schedule where code = '3' and part_number = 1)));
INSERT INTO offence_schedule_mapping (offence_id, schedule_part_id) values ((select id from offence o where o.code = 'DV00001'), (select id from schedule_part where schedule_id = (select id from schedule where code = 'DVEO' and part_number = 1)));
INSERT INTO offence_schedule_mapping (offence_id, schedule_part_id) values ((select id from offence o where o.code = 'TR00001'), (select id from schedule_part where schedule_id = (select id from schedule where code = 'TEO' and part_number = 1)));
INSERT INTO offence_schedule_mapping (offence_id, schedule_part_id, legislation_text) values ((select id from offence o where o.code = 'SA00001'), (select id from schedule_part where schedule_id = (select id from schedule where code = 'SEO' and part_number = 1)), 'SA0001 legislation');
