-- Remove obsolete migration-only schedule sync toggles
DELETE FROM feature_toggle
WHERE feature IN ('UNLINK_SCHEDULES_NOMIS', 'LINK_SCHEDULES_NOMIS');

DELETE FROM offence_to_sync_with_nomis
WHERE nomis_sync_type IN ('UNLINK_SCHEDULE_FROM_OFFENCE', 'LINK_SCHEDULE_TO_OFFENCE');
