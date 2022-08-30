ALTER TABLE offence ADD COLUMN parent_offence_id INT;
ALTER TABLE offence ADD CONSTRAINT fk_offence_to_parent_offence FOREIGN KEY (parent_offence_id) REFERENCES offence (id);

UPDATE offence o set parent_offence_id = (SELECT id FROM offence o2 WHERE o2.code = SUBSTRING(o.code, 1, 7))
WHERE length(o.code) > 7;
