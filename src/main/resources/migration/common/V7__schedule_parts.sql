CREATE TABLE schedule
(
    id   INT GENERATED ALWAYS AS IDENTITY,
    act  VARCHAR(1024),
    code VARCHAR(50),
    url  VARCHAR(1024),
    PRIMARY KEY (id)
);


CREATE TABLE schedule_part
(
    id          INT GENERATED ALWAYS AS IDENTITY,
    schedule_id INT,
    part_number INT,
    CONSTRAINT fk_schedule_part_to_schedule
        FOREIGN KEY (schedule_id)
            REFERENCES schedule (id),
    PRIMARY KEY (id)
);

CREATE TABLE offence_schedule_part
(
    id               INT GENERATED ALWAYS AS IDENTITY,
    schedule_part_id INT,
    offence_id       INT,
    CONSTRAINT fk_schedule_to_offence
        FOREIGN KEY (offence_id)
            REFERENCES offence (id),
    CONSTRAINT fk_schedule_part_mapping
        FOREIGN KEY (schedule_part_id)
            REFERENCES schedule_part (id),
    PRIMARY KEY (id)
);


ALTER TABLE offence ADD CONSTRAINT unique_offence_code UNIQUE (code);
ALTER TABLE schedule ADD CONSTRAINT unique_schedule UNIQUE (act, code);
ALTER TABLE schedule_part ADD CONSTRAINT unique_schedule_part UNIQUE (schedule_id, part_number);
ALTER TABLE offence_schedule_part ADD CONSTRAINT unique_offence_schedule_part UNIQUE (schedule_part_id, offence_id);