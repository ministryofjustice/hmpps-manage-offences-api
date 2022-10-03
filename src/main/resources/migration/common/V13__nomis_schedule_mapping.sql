CREATE TABLE nomis_schedule_mapping
(
    id                   INT GENERATED ALWAYS AS IDENTITY,
    schedule_part_id     INT                      NOT NULL,
    nomis_schedule_name  VARCHAR(30)              NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE nomis_schedule_mapping ADD CONSTRAINT fk_nomis_schedule_mapping FOREIGN KEY (schedule_part_id) REFERENCES schedule_part (id);

CREATE INDEX idx_nomis_schedule_mapping ON nomis_schedule_mapping (schedule_part_id);

INSERT INTO nomis_schedule_mapping (nomis_schedule_name, schedule_part_id) VALUES  
    ('SCHEDULE_13', (SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = '13') and part_number = 1)),
    ('SCHEDULE_13', (SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = '13') and part_number = 2)),
    ('SCHEDULE_15', (SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = '15') and part_number = 1)),
    ('SCHEDULE_15', (SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = '15') and part_number = 2)),
    ('SCHEDULE_15', (SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = '15') and part_number = 3)),
    ('SCHEDULE_17A_PART_1', (SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = '17A') and part_number = 1)),
    ('SCHEDULE_17A_PART_2', (SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = '17A') and part_number = 2)),
    ('SCHEDULE_19ZA', (SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = '19ZA') and part_number = 1)),
    ('SCHEDULE_19ZA', (SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = '19ZA') and part_number = 2)),
    ('SCHEDULE_19ZA', (SELECT id FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = '19ZA') and part_number = 3));
