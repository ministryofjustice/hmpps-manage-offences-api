CREATE TABLE offence_to_schedule_history
(
    id                   INT GENERATED ALWAYS AS IDENTITY,
    schedule_code        VARCHAR(50)              NOT NULL,
    schedule_part_id     INT                      NOT NULL,
    schedule_part_number INT                      NOT NULL,
    offence_id           INT                      NOT NULL,
    offence_code         VARCHAR(30)              NOT NULL,
    change_type          VARCHAR(30)              NOT NULL,
    pushed_to_nomis      boolean                  NOT NULL,
    created_date         TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_pushed_to_nomis ON offence_to_schedule_history (pushed_to_nomis);
