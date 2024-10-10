INSERT INTO schedule (code, act, url)
VALUES ('19ZA', 'test', '');
INSERT INTO schedule_part (schedule_id, part_number)
VALUES ((select id from schedule order by id desc limit 1), 1);

INSERT INTO offence (code, description, revision_id, cjs_title, start_date, end_date, changed_date, created_date,
                     last_updated_date, acts_and_sections)
VALUES ('AO06999', 'Brought before the court as being absent without leave from the Armed Forces', 570173,
        'Brought before the court as being absent without leave from the Armed Forces', '2009-11-02', NULL,
        '2020-01-16 15:19:02.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178',
        'Contrary to section 19 of the Zoo Licensing Act 1981'),
       ('AO07000', 'Torera related offence', 670173,
        'Torera related offence', '2009-11-02', NULL,
        '2020-01-16 15:19:02.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178',
        'Torera related offence'),
       ('AO07001', 'Torera related offence', 770173,
        'Torera related offence', '2009-11-02', NULL,
        '2020-01-16 15:19:02.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178',
        'Torera related offence'),
       ('AO07002', 'Torera related offence', 770175,
        'Node Torera related offence', '2009-11-02', NULL,
        '2020-01-16 15:19:02.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178',
        'None Torera related offence');

INSERT INTO offence_schedule_mapping (schedule_part_id, paragraph_number, paragraph_title, line_reference,
                                      legislation_text, offence_id)
VALUES ((select id from schedule_part order by id desc limit 1), 'p1', 'p_title', 'l1', 'leg1',
        (select id from offence o where o.code = 'AO07000'));

INSERT INTO schedule_part (schedule_id, part_number)
VALUES ((select id from schedule order by id desc limit 1), 2);

INSERT INTO offence_schedule_mapping (schedule_part_id, paragraph_number, paragraph_title, line_reference,
                                      legislation_text, offence_id)
VALUES ((select id from schedule_part order by id desc limit 1), 'p1', 'p_title', 'l1', 'leg1',
        (select id from offence o where o.code = 'AO07001'));

INSERT INTO schedule_part (schedule_id, part_number)
VALUES ((select id from schedule order by id desc limit 1), 3);

INSERT INTO offence_schedule_mapping (schedule_part_id, paragraph_number, paragraph_title, line_reference,
                                      legislation_text, offence_id)
VALUES ((select id from schedule_part order by id desc limit 1), 'p1', 'p_title', 'l1', 'leg1',
        (select id from offence o where o.code = 'AO07002'));
