INSERT INTO offence
(code, description, revision_id, cjs_title, start_date, end_date, changed_date, created_date, last_updated_date, custodial_indicator, sdrs_cache)
VALUES ('XX99001',
        'Fail to give to an authorised person information / assistance / provide facilities that person may require',
        574449,
        'CJS Title Fail to give to an authorised person information / assistance / provide facilities that person may require',
        '2015-03-13', NULL, '2020-06-17 15:31:26.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178', 2, 'GET_APPLICATIONS');

INSERT INTO offence
(code, description, revision_id, cjs_title, start_date, end_date, changed_date, created_date, last_updated_date, custodial_indicator, sdrs_cache)
VALUES ('XX99002',
        '2Fail to give to an authorised person information / assistance / provide facilities that person may require',
        574449,
        'CJS Title 2Fail to give to an authorised person information / assistance / provide facilities that person may require',
        '2015-03-13', NULL, '2020-06-17 15:31:26.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178', 0, 'GET_APPLICATIONS');

INSERT INTO offence_schedule_mapping (schedule_part_id, offence_id)
VALUES ((select id from schedule_part where schedule_id = (select id from schedule where code = '13' and part_number = 1)),
        (select id from offence where code = 'XX99001'));
