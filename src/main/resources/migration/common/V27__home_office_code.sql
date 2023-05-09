CREATE TABLE home_office_code (
    id VARCHAR(5),
    category INT NOT NULL,
    sub_category INT NOT NULL,
    description VARCHAR(1024),
    PRIMARY KEY (id)
);

INSERT INTO feature_toggle (feature, enabled) VALUES ('SYNC_HOME_OFFICE_CODES', false);
