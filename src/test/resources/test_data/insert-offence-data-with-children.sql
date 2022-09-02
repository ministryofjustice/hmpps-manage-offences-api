INSERT INTO public.offence
(code, description, revision_id, cjs_title, start_date, end_date, changed_date, created_date, last_updated_date, acts_and_sections)
VALUES('AF06999', 'Brought before the court as being absent without leave from the Armed Forces', 570173, 'Brought before the court as being absent without leave from the Armed Forces', '2009-11-02', NULL, '2020-01-16 15:19:02.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178', 'Contrary to section 19 of the Zoo Licensing Act 1981');
INSERT INTO public.offence
(code, description, revision_id, cjs_title, start_date, end_date, changed_date, created_date, last_updated_date, acts_and_sections)
VALUES('AF06999A', 'Inchoate A', 570173, 'Inchoate A', '2009-11-02', NULL, '2020-01-16 15:19:02.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178', 'Contrary to section 19 of the Zoo Licensing Act 1981');
INSERT INTO public.offence
(code, description, revision_id, cjs_title, start_date, end_date, changed_date, created_date, last_updated_date, acts_and_sections)
VALUES('AF06999B', 'Inchoate B', 570173, 'Inchoate B', '2009-11-02', NULL, '2020-01-16 15:19:02.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178', 'Contrary to section 19 of the Zoo Licensing Act 1981');
INSERT INTO public.offence
(code, description, revision_id, cjs_title, start_date, end_date, changed_date, created_date, last_updated_date, acts_and_sections)
VALUES('AF06999C', 'Inchoate C', 570173, 'Inchoate C', '2009-11-02', NULL, '2020-01-16 15:19:02.000', '2022-04-07 16:05:58.178', '2022-04-07 16:05:58.178', 'Contrary to section 19 of the Zoo Licensing Act 1981');

UPDATE offence a SET a.parent_offence_id = (SELECT id FROM offence b where b.code = 'AF06999') WHERE code IN ('AF06999A', 'AF06999B', 'AF06999C');