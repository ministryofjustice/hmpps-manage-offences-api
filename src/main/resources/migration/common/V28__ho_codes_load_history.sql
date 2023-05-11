CREATE TABLE ho_codes_load_history (
    id INT GENERATED ALWAYS AS IDENTITY,
    loaded_file VARCHAR(512) NOT NULL,
    load_date TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);
