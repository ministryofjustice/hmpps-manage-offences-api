ALTER TABLE offence_to_sync_with_nomis ADD COLUMN nomis_schedule_name VARCHAR(1024);
ALTER TABLE offence_to_sync_with_nomis ALTER COLUMN nomis_sync_type TYPE VARCHAR(50);
ALTER TABLE offence_to_sync_with_nomis DROP CONSTRAINT unique_offence_id_and_type;

INSERT INTO feature_toggle (feature, enabled) VALUES ('UNLINK_SCHEDULES_NOMIS', false);
INSERT INTO feature_toggle (feature, enabled) VALUES ('LINK_SCHEDULES_NOMIS', false);

-- SCHEDULE_15 unlink legacy S15 mappings from nomis if the offence is not in s15 FROM HMCTS
INSERT INTO offence_to_sync_with_nomis
(offence_code, nomis_sync_type, nomis_schedule_name, created_date)
    (select o.code, 'UNLINK_SCHEDULE_TO_OFFENCE', 'SCHEDULE_15', NOW()
     from nomis_schedule_mapping nsm
              join schedule_part sp on sp.id = nsm.schedule_part_id
              join schedule s on s.id = sp.schedule_id
              join offence_schedule_mapping osm on osm.schedule_part_id = sp.id
              join offence o on osm.offence_id = o.id
     where s.code = 'PCSC and Legacy NOMIS'
       and sp.part_number in (4, 5,6)
       and o.code not in (
         select o2.code
         from offence_schedule_mapping osm15
                  join schedule_part sp15 on sp15.id = osm15.schedule_part_id
                  join schedule s15 on sp15.schedule_id = s15.id
                  join offence o2 on osm15.offence_id = o2.id
         where s15.code = '15'
           and sp15.part_number in (1,2, 3)
     )
     order by o.code );

-- SCHEDULE_15 link to nomis where in s15 part 1 but not already linked (could already be linked by the legacy mappings)
INSERT INTO offence_to_sync_with_nomis
(offence_code, nomis_sync_type, nomis_schedule_name, created_date)
    (select o2.code, 'LINK_SCHEDULE_TO_OFFENCE', 'SCHEDULE_15', NOW()
     from offence_schedule_mapping osm15
              join schedule_part sp15 on sp15.id = osm15.schedule_part_id
              join schedule s15 on sp15.schedule_id = s15.id
              join offence o2 on osm15.offence_id = o2.id
     where s15.code = '15'
       and sp15.part_number in (1)
       and o2.code not in (select o.code
                           from nomis_schedule_mapping nsm
                                    join schedule_part sp on sp.id = nsm.schedule_part_id
                                    join schedule s on s.id = sp.schedule_id
                                    join offence_schedule_mapping osm on osm.schedule_part_id = sp.id
                                    join offence o on osm.offence_id = o.id
                           where s.code = 'PCSC and Legacy NOMIS'
                             and sp.part_number in (4, 5,6))
     order by o2.code );

-- SCHEDULE_15 link to nomis where in s15 part 2 but not already linked (could already be linked by the legacy mappings)
INSERT INTO offence_to_sync_with_nomis
(offence_code, nomis_sync_type, nomis_schedule_name, created_date)
    (select o2.code, 'LINK_SCHEDULE_TO_OFFENCE', 'SCHEDULE_15', NOW()
     from offence_schedule_mapping osm15
              join schedule_part sp15 on sp15.id = osm15.schedule_part_id
              join schedule s15 on sp15.schedule_id = s15.id
              join offence o2 on osm15.offence_id = o2.id
     where s15.code = '15'
       and sp15.part_number in (2)
       and o2.code not in (select o.code
                           from nomis_schedule_mapping nsm
                                    join schedule_part sp on sp.id = nsm.schedule_part_id
                                    join schedule s on s.id = sp.schedule_id
                                    join offence_schedule_mapping osm on osm.schedule_part_id = sp.id
                                    join offence o on osm.offence_id = o.id
                           where s.code = 'PCSC and Legacy NOMIS'
                             and sp.part_number in (4, 5,6))
       and not exists (
         SELECT NULL
         FROM offence_to_sync_with_nomis  otsn
         WHERE otsn.nomis_sync_type = 'LINK_SCHEDULE_TO_OFFENCE'
           AND otsn.nomis_schedule_name = 'SCHEDULE_15'
           AND otsn.offence_code = o2.code
        )
     order by o2.code );

-- PCSC_SDS, PCSC_SDS_PLUS, PCSC_SEC_250, SCHEDULE_15_ATTRACTS_LIFE unlink all from NOMIS
INSERT INTO offence_to_sync_with_nomis
(offence_code, nomis_sync_type, nomis_schedule_name, created_date)
    (select o.code, 'UNLINK_SCHEDULE_TO_OFFENCE', CASE WHEN sp.part_number = 1 THEN 'PCSC_SDS'
                                                               WHEN sp.part_number = 2 THEN 'PCSC_SDS_PLUS'
                                                               WHEN sp.part_number = 3 THEN 'PCSC_SEC_250'
                                                               WHEN sp.part_number = 7 THEN 'SCHEDULE_15_ATTRACTS_LIFE'
                                                            END
        , NOW()
     from nomis_schedule_mapping nsm
              join schedule_part sp on sp.id = nsm.schedule_part_id
              join schedule s on s.id = sp.schedule_id
              join offence_schedule_mapping osm on osm.schedule_part_id = sp.id
              join offence o on osm.offence_id = o.id
     where s.code = 'PCSC and Legacy NOMIS'
       and sp.part_number in (1,2,3,7)
     order by o.code );

-- schedule 15 potentially link all from parts 1 and 2 to NOMIS PCSC indicators (actual pcsc check done in code)
INSERT INTO offence_to_sync_with_nomis
(offence_code, nomis_sync_type, nomis_schedule_name, created_date)
    (select o.code, 'LINK_SCHEDULE_TO_OFFENCE', 'POTENTIAL_LINK_PCSC' , NOW()
     from nomis_schedule_mapping nsm
              join schedule_part sp on sp.id = nsm.schedule_part_id
              join schedule s on s.id = sp.schedule_id
              join offence_schedule_mapping osm on osm.schedule_part_id = sp.id
              join offence o on osm.offence_id = o.id
     where s.code = '15'
       and sp.part_number in (1,2)
     order by o.code );

DELETE FROM offence_schedule_mapping
       WHERE schedule_part_id IN (SELECT id FROM schedule_part sp WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'PCSC and Legacy NOMIS'));

DELETE FROM nomis_schedule_mapping WHERE schedule_part_id IN (SELECT id FROM schedule_part sp WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'PCSC and Legacy NOMIS'));

DELETE FROM schedule_part WHERE schedule_id = (SELECT id FROM schedule WHERE code = 'PCSC and Legacy NOMIS');

DELETE FROM schedule WHERE code = 'PCSC and Legacy NOMIS';

INSERT INTO nomis_schedule_mapping (nomis_schedule_name, schedule_part_id)
VALUES ('SCHEDULE_15', (SELECT id
                     FROM schedule_part
                     WHERE schedule_id = (SELECT id FROM schedule WHERE code = '15') AND part_number = 1)),
       ('SCHEDULE_15', (SELECT id
                          FROM schedule_part
                          WHERE schedule_id = (SELECT id FROM schedule WHERE code = '15') AND part_number = 2));
