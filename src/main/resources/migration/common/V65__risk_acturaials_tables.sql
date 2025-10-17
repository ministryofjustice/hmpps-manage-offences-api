CREATE TABLE IF NOT EXISTS risk_actuarial_ho_code (
  id INT GENERATED ALWAYS AS IDENTITY,
  category INT NOT NULL,
  sub_category INT NOT NULL,
  created_date TIMESTAMP WITH TIME ZONE NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (category, sub_category)
);

CREATE TABLE IF NOT EXISTS risk_actuarial_ho_code_weightings (
  id INT GENERATED ALWAYS AS IDENTITY,
  risk_actuarial_ho_code_id INT NOT NULL,
  weighting_name varchar(30) NOT NULL,
  weighting_value NUMERIC,
  weighting_desc TEXT NOT NULL,
  error_code varchar(60),
  PRIMARY KEY (id),
  CONSTRAINT fk_risk_actuarial_ho_code
    FOREIGN KEY(risk_actuarial_ho_code_id)
    REFERENCES risk_actuarial_ho_code(id),
  UNIQUE (risk_actuarial_ho_code_id, weighting_name)
);

CREATE TABLE IF NOT EXISTS risk_actuarial_ho_code_flags (
  id INT GENERATED ALWAYS AS IDENTITY,
  risk_actuarial_ho_code_id INT NOT NULL,
  flag_name varchar(30) NOT NULL,
  flag_value BOOLEAN,
  created_date TIMESTAMP WITH TIME ZONE NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_risk_actuarial_ho_code
    FOREIGN KEY(risk_actuarial_ho_code_id)
    REFERENCES risk_actuarial_ho_code(id),
  UNIQUE (risk_actuarial_ho_code_id, flag_name)
);
