INSERT INTO schedule (act, code, url)
VALUES ('Criminal Justice Act 2003', 'PCSC', 'https://www.legislation.gov.uk/ukpga/2003/44/schedule/15');

INSERT INTO schedule_part (schedule_id, part_number)
VALUES ((SELECT id FROM schedule WHERE code = 'PCSC'), 1),
       ((SELECT id FROM schedule WHERE code = 'PCSC'), 2),
       ((SELECT id FROM schedule WHERE code = 'PCSC'), 3);

INSERT INTO nomis_schedule_mapping (nomis_schedule_name, schedule_part_id)
VALUES ('PCSC_SDS', (SELECT id
                     FROM schedule_part
                     WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'PCSC') AND part_number = 1)),
       ('PCSC_SDS_PLUS', (SELECT id
                          FROM schedule_part
                          WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'PCSC') AND part_number = 2)),
       ('PCSC_SEC_250', (SELECT id
                         FROM schedule_part
                         WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'PCSC') AND part_number = 3));
