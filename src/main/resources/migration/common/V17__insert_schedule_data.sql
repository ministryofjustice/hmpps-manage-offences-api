INSERT INTO schedule (act, code, url)
VALUES ('Sentencing Act 2020', '1', 'https://www.legislation.gov.uk/ukpga/2020/17/schedule/1'),
       ('Sentencing Act 2020', 'A1', 'https://www.legislation.gov.uk/ukpga/2020/17/schedule/A1');

INSERT INTO schedule_part (schedule_id, part_number)
VALUES ((SELECT id FROM schedule WHERE code = '1'), 1),
       ((SELECT id FROM schedule WHERE code = 'A1'), 1);
