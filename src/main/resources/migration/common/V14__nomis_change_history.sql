CREATE TABLE nomis_change_history
(
    id                 INT GENERATED ALWAYS AS IDENTITY,
    code               VARCHAR(30)              NOT NULL,
    description        VARCHAR(1024)            NOT NULL,
    change_type        VARCHAR(30)              NOT NULL,
    nomis_change_type  VARCHAR(30)              NOT NULL,
    sent_to_nomis_date TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_sent_to_nomis_date ON nomis_change_history (sent_to_nomis_date);
