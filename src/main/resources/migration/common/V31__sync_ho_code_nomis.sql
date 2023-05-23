CREATE TABLE offence_to_sync_with_nomis
(
    id               INT GENERATED ALWAYS AS IDENTITY,
    offence_id       INT NOT NULL,
    type             VARCHAR(20) NOT NULL,
    offence_end_date DATE,
    CONSTRAINT fk_offence_to_sync_with_nomis_constraint
        FOREIGN KEY (offence_id)
            REFERENCES offence (id),
    PRIMARY KEY (id)
);

ALTER TABLE offence_to_sync_with_nomis ADD CONSTRAINT unique_offence_id_and_type UNIQUE (offence_id, type);
comment on table offence_to_sync_with_nomis is 'This table is used to synchronise NOMIS with changes that are not covered by the delta sync; e.g. hho0code updates';

CREATE TABLE previous_offence_to_ho_code_mapping
(
    offence_id       INT NOT NULL,
    category INT NOT NULL,
    sub_category INT NOT NULL,
    PRIMARY KEY (offence_id)
);
