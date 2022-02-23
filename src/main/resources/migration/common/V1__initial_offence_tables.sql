CREATE TABLE IF NOT EXISTS offence (
  id INT GENERATED ALWAYS AS IDENTITY,
  code varchar(30),
  description varchar(1024),
  PRIMARY KEY (id)
);

-- A Group type would be a schedule
CREATE TABLE IF NOT EXISTS offence_group_type (
  id INT GENERATED ALWAYS AS IDENTITY,
  type varchar(30),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS offence_group (
  id INT GENERATED ALWAYS AS IDENTITY,
  type_id INT,
  offence_id INT,
  PRIMARY KEY (id),
  CONSTRAINT fk_offence_group_type
    FOREIGN KEY(type_id)
    REFERENCES offence_group_type(id),
  CONSTRAINT fk_offence
    FOREIGN KEY(offence_id)
    REFERENCES offence(id)
);
