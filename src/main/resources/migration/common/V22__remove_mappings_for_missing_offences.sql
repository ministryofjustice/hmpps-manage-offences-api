-- In the DEV environment (which is loaded by staging sdrs) not all the offences exist, therefore some of the ref data from the schedules load needs to be deleted
DELETE FROM offence_schedule_mapping WHERE offence_id IS NULL;
ALTER TABLE offence_schedule_mapping ALTER COLUMN offence_id SET NOT NULL;
DROP TABLE offence_schedule_part;
DROP TABLE offence_to_schedule_history;
