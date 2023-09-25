UPDATE schedule
SET code = 'PCSC and Legacy NOMIS'
WHERE code = 'PCSC';

INSERT INTO schedule_part (schedule_id, part_number)
VALUES ((SELECT id FROM schedule WHERE code = 'PCSC and Legacy NOMIS'), 4); -- schedule 15 Part 1
INSERT INTO schedule_part (schedule_id, part_number)
VALUES ((SELECT id FROM schedule WHERE code = 'PCSC and Legacy NOMIS'), 5); -- schedule 15 Part 2
INSERT INTO schedule_part (schedule_id, part_number)
VALUES ((SELECT id FROM schedule WHERE code = 'PCSC and Legacy NOMIS'), 6); -- schedule 15 Part 3
INSERT INTO schedule_part (schedule_id, part_number)
VALUES ((SELECT id FROM schedule WHERE code = 'PCSC and Legacy NOMIS'), 7); -- schedule 15 attracts life

-- Schedule 15 Part 1
UPDATE nomis_schedule_mapping
SET schedule_part_id = (SELECT id
                        FROM schedule_part
                        WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'PCSC and Legacy NOMIS')
                          AND part_number = 4)
WHERE schedule_part_id = (SELECT id
                          FROM schedule_part
                          WHERE schedule_id = (SELECT id FROM schedule WHERE code = '15')
                            AND part_number = 1);

-- Schedule 15 Part 2
UPDATE nomis_schedule_mapping
SET schedule_part_id = (SELECT id
                        FROM schedule_part
                        WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'PCSC and Legacy NOMIS')
                          AND part_number = 5)
WHERE schedule_part_id = (SELECT id
                          FROM schedule_part
                          WHERE schedule_id = (SELECT id FROM schedule WHERE code = '15')
                            AND part_number = 2);

-- Schedule 15 Part 3
UPDATE nomis_schedule_mapping
SET schedule_part_id = (SELECT id
                        FROM schedule_part
                        WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'PCSC and Legacy NOMIS')
                          AND part_number = 6)
WHERE schedule_part_id = (SELECT id
                          FROM schedule_part
                          WHERE schedule_id = (SELECT id FROM schedule WHERE code = '15')
                            AND part_number = 3);

-- Schedule 15 Attracts life Part 1
UPDATE nomis_schedule_mapping
SET schedule_part_id = (SELECT id
                        FROM schedule_part
                        WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'PCSC and Legacy NOMIS')
                          AND part_number = 7)
WHERE schedule_part_id = (SELECT id
                          FROM schedule_part
                          WHERE schedule_id = (SELECT id FROM schedule WHERE code = '15-LIFE')
                            AND part_number = 1);

-- UPDATE  existing schedule 15 Part 1 to go to the nomis legacy schedule
UPDATE offence_schedule_mapping
SET schedule_part_id = (SELECT schedule_part_id
                        FROM nomis_schedule_mapping
                        WHERE schedule_part_id =
                              (SELECT id
                               FROM schedule_part
                               WHERE schedule_id =
                                     (SELECT id FROM schedule WHERE code = 'PCSC and Legacy NOMIS')
                                 AND part_number = 4))
WHERE schedule_part_id =
      (SELECT id
       FROM schedule_part
       WHERE schedule_id =
             (SELECT id FROM schedule WHERE code = '15')
         AND part_number = 1);

-- UPDATE  existing schedule 15 Part 2 to go to the nomis legacy schedule
UPDATE offence_schedule_mapping
SET schedule_part_id = (SELECT schedule_part_id
                        FROM nomis_schedule_mapping
                        WHERE schedule_part_id =
                              (SELECT id
                               FROM schedule_part
                               WHERE schedule_id =
                                     (SELECT id FROM schedule WHERE code = 'PCSC and Legacy NOMIS')
                                 AND part_number = 5))
WHERE schedule_part_id =
      (SELECT id
       FROM schedule_part
       WHERE schedule_id =
             (SELECT id FROM schedule WHERE code = '15')
         AND part_number = 2);

-- UPDATE  existing schedule 15 Part 3 to go to the nomis legacy schedule
UPDATE offence_schedule_mapping
SET schedule_part_id = (SELECT schedule_part_id
                        FROM nomis_schedule_mapping
                        WHERE schedule_part_id =
                              (SELECT id
                               FROM schedule_part
                               WHERE schedule_id =
                                     (SELECT id FROM schedule WHERE code = 'PCSC and Legacy NOMIS')
                                 AND part_number = 6))
WHERE schedule_part_id =
      (SELECT id
       FROM schedule_part
       WHERE schedule_id =
             (SELECT id FROM schedule WHERE code = '15')
         AND part_number = 3);

-- UPDATE  existing schedule 15-LIFE Part 1 to go to the nomis legacy schedule
UPDATE offence_schedule_mapping
SET schedule_part_id = (SELECT schedule_part_id
                        FROM nomis_schedule_mapping
                        WHERE schedule_part_id =
                              (SELECT id
                               FROM schedule_part
                               WHERE schedule_id =
                                     (SELECT id FROM schedule WHERE code = 'PCSC and Legacy NOMIS')
                                 AND part_number = 7))
WHERE schedule_part_id =
      (SELECT id
       FROM schedule_part
       WHERE schedule_id =
             (SELECT id FROM schedule WHERE code = '15-LIFE')
         AND part_number = 1);

DELETE
FROM schedule_part
WHERE schedule_id = (SELECT id FROM schedule WHERE code = '15-LIFE');

DELETE
FROM schedule
WHERE code = '15-LIFE';
