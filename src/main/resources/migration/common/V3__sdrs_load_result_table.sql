CREATE TABLE IF NOT EXISTS sdrs_load_result (
  alpha_char CHAR(1),
  status VARCHAR(32),
  load_type VARCHAR(32),
  load_date TIMESTAMP WITH TIME ZONE,
  last_successful_load_date TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (alpha_char)
);

CREATE TABLE IF NOT EXISTS sdrs_load_result_history (
  id INT GENERATED ALWAYS AS IDENTITY,
  alpha_char CHAR(1),
  status VARCHAR(32),
  load_type VARCHAR(32),
  load_date TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (id)
);

INSERT INTO sdrs_load_result (alpha_char) VALUES ('A');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('B');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('C');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('D');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('E');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('F');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('G');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('H');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('I');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('J');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('K');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('L');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('M');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('N');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('O');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('P');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('Q');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('R');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('S');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('T');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('U');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('V');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('W');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('X');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('Y');
INSERT INTO sdrs_load_result (alpha_char) VALUES ('Z');
