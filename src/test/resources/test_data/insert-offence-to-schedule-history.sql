UPDATE sdrs_load_result SET last_successful_load_date = now();

INSERT INTO offence
(code, description, revision_id, cjs_title, start_date, end_date, changed_date, created_date, last_updated_date)
VALUES ('XX99001',
        'Fail to give to an authorised person information / assistance / provide facilities that person may require',
        574449,
        'CJS Title Fail to give to an authorised person information / assistance / provide facilities that person may require',
        '2015-03-13', NULL, '2020-06-17 15:31:26.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178');

INSERT INTO offence
(code, description, revision_id, cjs_title, start_date, end_date, changed_date, created_date, last_updated_date)
VALUES ('XX99002',
        '2Fail to give to an authorised person information / assistance / provide facilities that person may require',
        574449,
        'CJS Title 2Fail to give to an authorised person information / assistance / provide facilities that person may require',
        '2015-03-13', NULL, '2020-06-17 15:31:26.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178');

INSERT INTO offence_schedule_part (schedule_part_id, offence_id)
VALUES ((select id
         from schedule_part
         where schedule_id = (select id from schedule where code = '15' and part_number = 1)),
        (select id from offence where code = 'XX99001'));
INSERT INTO offence_schedule_part (schedule_part_id, offence_id)
VALUES ((select id
         from schedule_part
         where schedule_id = (select id from schedule where code = '15' and part_number = 2)),
        (select id from offence where code = 'XX99001'));
INSERT INTO offence_schedule_part (schedule_part_id, offence_id)
VALUES ((select id
         from schedule_part
         where schedule_id = (select id from schedule where code = '13' and part_number = 1)),
        (select id from offence where code = 'XX99001'));
INSERT INTO offence_schedule_part (schedule_part_id, offence_id)
VALUES ((select id
         from schedule_part
         where schedule_id = (select id from schedule where code = '15' and part_number = 1)),
        (select id from offence where code = 'XX99002'));

INSERT INTO offence_to_schedule_history (schedule_code, schedule_part_id, schedule_part_number, offence_id,
                                         offence_code, change_type, pushed_to_nomis, created_date)
VALUES ('13',
        (select id
         from schedule_part
         where schedule_id = (select id from schedule where code = '13' and part_number = 1)),
        1,
        (select id from offence where code = 'XX99001'),
        'XX99002', 'DELETE', false, '2022-10-03 15:37:12.574849+01'),
        ('13',
        (select id
         from schedule_part
         where schedule_id = (select id from schedule where code = '13' and part_number = 1)),
        1,
        (select id from offence where code = 'XX99001'),
        'XX99002', 'INSERT', false, '2022-10-03 15:37:13.574849+01'),
        ('13',
        (select id
         from schedule_part
         where schedule_id = (select id from schedule where code = '13' and part_number = 1)),
        1,
        (select id from offence where code = 'XX99001'),
        'XX99001', 'INSERT', false, '2022-10-03 15:37:14.574849+01'),
        ('13',
        (select id
         from schedule_part
         where schedule_id = (select id from schedule where code = '13' and part_number = 1)),
        1,
        (select id from offence where code = 'XX99001'),
        'XX99001', 'DELETE', false, '2022-10-03 15:37:15.574849+01'),
        ('13',
        (select id
         from schedule_part
         where schedule_id = (select id from schedule where code = '13' and part_number = 1)),
        1,
        (select id from offence where code = 'XX99001'),
        'XX99001', 'INSERT', false, '2022-10-03 15:37:16.574849+01'),
        ('13',
        (select id
         from schedule_part
         where schedule_id = (select id from schedule where code = '13' and part_number = 1)),
        1,
        (select id from offence where code = 'XX99001'),
        'XX99001', 'DELETE', false, '2022-10-03 15:37:17.574849+01');
