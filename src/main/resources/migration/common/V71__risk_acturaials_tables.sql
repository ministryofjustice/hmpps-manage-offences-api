DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'actuarial_category') THEN
        CREATE TYPE actuarial_category AS ENUM (
            'UNKNOWN',
            'BURGLARY_DOMESTIC',
            'BURGLARY_OTHER',
            'DRUNKENNESS',
            'DRINK_DRIVING',
            'MOTORING_OFFENCES',
            'VEHICLE_RELATED_THEFT',
            'FRAUD_AND_FORGERY',
            'WELFARE_FRAUD',
            'DRUG_IMPORT_EXPORT_OR_PRODUCTION',
            'DRUG_POSSESSION_OR_SUPPLY',
            'VIOLENCE_AGAINST_THE_PERSON_ABH_PLUS',
            'VIOLENCE_AGAINST_THE_PERSON_SUB_ABH',
            'PUBLIC_ORDER_AND_HARRASSMENT',
            'WEAPONS_NON_FIREARM',
            'FIREARMS_MOST_SERIOUS',
            'FIREARMS_OTHER',
            'HANDLING_STOLEN_GOODS',
            'CRIMINAL_DAMAGE',
            'ACQUISITIVE_VIOLENCE',
            'OTHER_OFFENCES',
            'ABSCONDING_OR_BAIL',
            'SEXUAL_AGAINST_CHILD',
            'SEXUAL_NOT_AGAINST_CHILD',
            'THEFT_NON_MOTOR',
            'NEED_DETAILS_OF_EXACT_OFFENCE'
        );
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS risk_actuarial_ho_code_category (
  id INT GENERATED ALWAYS AS IDENTITY,
  category_name actuarial_category NOT NULL,
  created_date TIMESTAMP WITH TIME ZONE NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (category_name)
);

CREATE TABLE IF NOT EXISTS risk_actuarial_ho_code (
  id INT GENERATED ALWAYS AS IDENTITY,
  category INT NOT NULL,
  sub_category INT NOT NULL,
  parent_group_description varchar(255) NOT NULL,
  category_description varchar(255) NOT NULL,
  sub_category_description varchar(255) NOT NULL,
  risk_actuarial_ho_code_category_id INT NOT NULL,
  created_date TIMESTAMP WITH TIME ZONE NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_risk_actuarial_ho_code_category
    FOREIGN KEY(risk_actuarial_ho_code_category_id)
    REFERENCES risk_actuarial_ho_code_category(id),
  UNIQUE (category, sub_category)
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
