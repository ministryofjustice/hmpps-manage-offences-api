CREATE TABLE legacy_sdrs_ho_code_mapping (
    offence_code varchar(30),
    category INT,
    sub_category INT,
    PRIMARY KEY (offence_code)
);

INSERT INTO legacy_sdrs_ho_code_mapping
    (SELECT code, category, sub_category FROM offence WHERE category IS NOT NULL OR sub_category IS NOT NULL);

UPDATE offence SET category = NULL, sub_category = NULL;
