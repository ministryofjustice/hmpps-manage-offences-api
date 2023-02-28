drop table offence_schedule_mapping;
drop table schedule_paragraph;

CREATE TABLE offence_schedule_mapping
(
    id               INT GENERATED ALWAYS AS IDENTITY,
    schedule_part_id INT NOT NULL ,
    paragraph_number INT,
    paragraph_title VARCHAR(256),
    line_reference VARCHAR(256),
    legislation_text VARCHAR(1024),
    offence_id       INT ,
    CONSTRAINT fk_schedule_to_offence_constraint
        FOREIGN KEY (offence_id)
            REFERENCES offence (id),
    CONSTRAINT fk_offence_to_schedule_constraint
        FOREIGN KEY (schedule_part_id)
            REFERENCES schedule_part (id),
    PRIMARY KEY (id)
);

ALTER TABLE offence_schedule_mapping ADD CONSTRAINT unique_schedule_offence_mapping UNIQUE (schedule_part_id, offence_id);
comment on column offence_schedule_mapping.line_reference is 'Some lines in a paragraph have an associated line reference unique to the paragraph, e.g. a or b';
