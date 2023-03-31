ALTER TABLE sdrs_load_result ADD COLUMN nomis_sync_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sdrs_load_result_history ADD COLUMN nomis_sync_required BOOLEAN;
