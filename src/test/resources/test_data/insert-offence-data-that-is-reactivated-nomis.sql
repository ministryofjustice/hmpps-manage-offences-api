INSERT INTO offence
(code, description, revision_id, cjs_title, start_date, end_date, changed_date, created_date, last_updated_date, category, sub_category)
VALUES('M5119999', 'Manslaughter Updated', 570173, 'Manslaughter Updated', '2009-11-02', '2012-11-02', '2020-01-16 15:19:02.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178', 91, 81);

INSERT INTO offence_reactivated_in_nomis (offence_id, offence_code, reactivated_by_username, reactivated_date) VALUES ((SELECT id FROM offence WHERE code = 'M5119999'), 'M5119999', 'test-user', NOW());
