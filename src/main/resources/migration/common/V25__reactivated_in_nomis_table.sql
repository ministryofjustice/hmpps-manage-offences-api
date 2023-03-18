CREATE TABLE offence_reactivated_in_nomis (
    offence_id INT REFERENCES offence (id),
    reactivated_by_username VARCHAR(32) NOT NULL,
    reactivated_date TIMESTAMP WITH TIME ZONE  NOT NULL,
    PRIMARY KEY (offence_id)
);

comment on table offence_reactivated_in_nomis is 'Contains offences that have been end dated (i.e. inactive) - but have been overridden to be active in NOMIS';
