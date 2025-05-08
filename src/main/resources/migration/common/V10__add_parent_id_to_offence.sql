ALTER TABLE offence ADD COLUMN parent_offence_id INT;
ALTER TABLE offence ADD CONSTRAINT fk_offence_to_parent_offence FOREIGN KEY (parent_offence_id) REFERENCES offence (id);

UPDATE offence
SET parent_offence_id = (
    SELECT id FROM offence AS o2
    WHERE o2.code = SUBSTRING(offence.code, 1, 7)
)
WHERE LENGTH(code) > 7;
