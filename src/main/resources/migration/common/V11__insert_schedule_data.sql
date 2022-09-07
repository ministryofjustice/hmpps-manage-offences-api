INSERT INTO schedule (act, code, url)
VALUES ('Sentencing Act 2020', '13', 'https://www.legislation.gov.uk/ukpga/2020/17/schedule/13'),
       ('Criminal Justice Act 2003', '15', 'https://www.legislation.gov.uk/ukpga/2003/44/schedule/15'),
       ('Criminal Justice Act 2003', '15B', 'https://www.legislation.gov.uk/ukpga/2003/44/schedule/15B'),
       ('Sentencing Act 2020', '17A', 'https://www.legislation.gov.uk/ukpga/2020/17/schedule/17A'),
       ('Criminal Justice Act 2003', '19ZA', 'https://www.legislation.gov.uk/ukpga/2003/44/schedule/19ZA');

INSERT INTO schedule_part (schedule_id, part_number)
VALUES ((SELECT id FROM schedule WHERE code = '13'), 1),
       ((SELECT id FROM schedule WHERE code = '13'), 2),
       ((SELECT id FROM schedule WHERE code = '15'), 1),
       ((SELECT id FROM schedule WHERE code = '15'), 2),
       ((SELECT id FROM schedule WHERE code = '15'), 3),
       ((SELECT id FROM schedule WHERE code = '15B'), 1),
       ((SELECT id FROM schedule WHERE code = '17A'), 1),
       ((SELECT id FROM schedule WHERE code = '17A'), 2),
       ((SELECT id FROM schedule WHERE code = '19ZA'), 1),
       ((SELECT id FROM schedule WHERE code = '19ZA'), 2),
       ((SELECT id FROM schedule WHERE code = '19ZA'), 3);
