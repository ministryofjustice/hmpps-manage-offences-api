INSERT INTO schedule (act, code, url)
VALUES ('Criminal Justice Act 2003', '15-LIFE', 'https://www.legislation.gov.uk/ukpga/2003/44/schedule/15');

INSERT INTO schedule_part (schedule_id, part_number)
VALUES ((SELECT id FROM schedule WHERE code = '15-LIFE'), 1);

INSERT INTO nomis_schedule_mapping (nomis_schedule_name, schedule_part_id)
VALUES ('SCHEDULE_15_ATTRACTS_LIFE', (SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = '15-LIFE') AND part_number = 1));
