CREATE TABLE offence_to_sync_with_nomis
(
    id               INT GENERATED ALWAYS AS IDENTITY,
    offence_code       INT                      NOT NULL,
    nomis_sync_type  VARCHAR(20)              NOT NULL,
    created_date     TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE offence_to_sync_with_nomis
    ADD CONSTRAINT unique_offence_id_and_type UNIQUE (offence_id, nomis_sync_type);

comment on table offence_to_sync_with_nomis is 'This table is used to synchronise NOMIS with changes that are not covered by the delta sync; e.g. hho0code updates';

CREATE TABLE previous_offence_to_ho_code_mapping
(
    offence_code   INT NOT NULL,
    category     INT NOT NULL,
    sub_category INT NOT NULL,
    PRIMARY KEY (offence_id)
);
