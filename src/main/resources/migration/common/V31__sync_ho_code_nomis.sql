CREATE TABLE offence_to_sync_with_nomis
(
    id               INT GENERATED ALWAYS AS IDENTITY,
    offence_code     VARCHAR(30)              NOT NULL,
    nomis_sync_type  VARCHAR(20)              NOT NULL,
    created_date     TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE offence_to_sync_with_nomis
    ADD CONSTRAINT unique_offence_id_and_type UNIQUE (offence_code, nomis_sync_type);

comment on table offence_to_sync_with_nomis is 'This table is used to synchronise NOMIS with changes that are not covered by the delta sync; e.g. hho0code updates';

CREATE TABLE previous_offence_to_ho_code_mapping
(
    offence_code VARCHAR(30) NOT NULL,
    category     INT NOT NULL,
    sub_category INT NOT NULL,
    PRIMARY KEY (offence_code)
);

INSERT INTO offence_to_sync_with_nomis
(offence_code, nomis_sync_type, created_date)
(select code, 'FUTURE_END_DATED', NOW()
    FROM offence o WHERE o.end_date > TO_DATE('2022-11-01', 'YYY-MM-DD'));
