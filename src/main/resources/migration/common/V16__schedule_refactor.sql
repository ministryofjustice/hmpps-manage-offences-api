CREATE TABLE schedule_paragraph
(
    id          INT GENERATED ALWAYS AS IDENTITY,
    schedule_part_id INT,
    paragraph_number INT,
    paragraph_title VARCHAR(1024),
    CONSTRAINT fk_schedule_paragraph_to_part
        FOREIGN KEY (schedule_part_id)
            REFERENCES schedule_part (id),
    PRIMARY KEY (id)
);

comment on column schedule_paragraph.paragraph_title is 'Each paragraph can have a heading, invariably this is heading is the title of an alternative act';

CREATE TABLE offence_schedule_mapping
(
    id               INT GENERATED ALWAYS AS IDENTITY,
    schedule_paragraph_id INT,
    line_reference VARCHAR(256),
    legislation_text VARCHAR(1024),
    offence_id       INT,
    CONSTRAINT fk_schedule_to_offence_c
        FOREIGN KEY (offence_id)
            REFERENCES offence (id),
    CONSTRAINT fk_schedule_legislation_line_to_paragraph
        FOREIGN KEY (schedule_paragraph_id)
            REFERENCES schedule_paragraph (id),
    PRIMARY KEY (id)
);

comment on column offence_schedule_mapping.line_reference is 'Some lines in a paragraph have an associated line reference unique to the paragraph, e.g. a or b';

ALTER TABLE offence ADD COLUMN offence_type VARCHAR (256);
ALTER TABLE offence ADD COLUMN max_period_is_life boolean;
ALTER TABLE offence ADD COLUMN max_period_of_indictment INT;
