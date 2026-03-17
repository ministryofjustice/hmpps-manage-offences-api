INSERT INTO schedule (code, act, url)
SELECT '19ZA', 'test', ''
    WHERE NOT EXISTS (
    SELECT 1 FROM schedule WHERE act = 'test' AND code = '19ZA'
);

INSERT INTO schedule_part (schedule_id, part_number)
SELECT s.id, p.part_number
FROM schedule s
         JOIN (VALUES (1), (2), (3)) AS p(part_number) ON TRUE
WHERE s.act = 'test' AND s.code = '19ZA'
    ON CONFLICT (schedule_id, part_number) DO NOTHING;

INSERT INTO offence (code, description, revision_id, cjs_title, start_date, end_date, changed_date,
                     created_date, last_updated_date, acts_and_sections)
VALUES
    ('AO06999', 'Brought before the court as being absent without leave from the Armed Forces', 570173,
     'Brought before the court as being absent without leave from the Armed Forces', '2009-11-02', NULL,
     '2020-01-16 15:19:02.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178',
     'Contrary to section 19 of the Zoo Licensing Act 1981'),
    ('AO07000', 'Torera related offence', 670173,
     'Torera related offence', '2009-11-02', NULL,
     '2020-01-16 15:19:02.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178',
     'Torera related offence'),
    ('AO07001', 'Torera related offence', 770173,
     'Torera related offence', '2009-11-02', NULL,
     '2020-01-16 15:19:02.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178',
     'Torera related offence'),
    ('AO07002', 'Torera related offence', 770175,
     'Node Torera related offence', '2009-11-02', NULL,
     '2020-01-16 15:19:02.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178',
     'None Torera related offence')
    ON CONFLICT (code) DO NOTHING;

INSERT INTO offence_schedule_mapping
(schedule_part_id, paragraph_number, paragraph_title, line_reference, legislation_text, offence_id)
SELECT sp.id, 'p1', 'p_title', 'l1', 'leg1', o.id
FROM schedule s
         JOIN schedule_part sp ON sp.schedule_id = s.id AND sp.part_number = 1
         JOIN offence o ON o.code = 'AO07000'
WHERE s.act = 'test' AND s.code = '19ZA'
    ON CONFLICT DO NOTHING;

INSERT INTO offence_schedule_mapping
(schedule_part_id, paragraph_number, paragraph_title, line_reference, legislation_text, offence_id)
SELECT sp.id, 'p1', 'p_title', 'l1', 'leg1', o.id
FROM schedule s
         JOIN schedule_part sp ON sp.schedule_id = s.id AND sp.part_number = 2
         JOIN offence o ON o.code = 'AO07001'
WHERE s.act = 'test' AND s.code = '19ZA'
    ON CONFLICT DO NOTHING;

INSERT INTO offence_schedule_mapping
(schedule_part_id, paragraph_number, paragraph_title, line_reference, legislation_text, offence_id)
SELECT sp.id, 'p1', 'p_title', 'l1', 'leg1', o.id
FROM schedule s
         JOIN schedule_part sp ON sp.schedule_id = s.id AND sp.part_number = 3
         JOIN offence o ON o.code = 'AO07002'
WHERE s.act = 'test' AND s.code = '19ZA'
    ON CONFLICT DO NOTHING;
