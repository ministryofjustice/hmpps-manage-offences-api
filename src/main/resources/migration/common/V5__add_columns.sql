ALTER TABLE offence ADD COLUMN category INT;
ALTER TABLE offence ADD COLUMN sub_category INT;
ALTER TABLE offence ADD COLUMN acts_and_sections varchar(1024);


UPDATE offence set category = substring(home_office_stats_code,1, position('/' in home_office_stats_code) - 1)::INTEGER
, sub_category = substring(home_office_stats_code, position('/' in home_office_stats_code) + 1)::INTEGER
WHERE home_office_stats_code like '%/%'
  and home_office_stats_code not like '%/'
  and home_office_stats_code not like '/%';


UPDATE offence set category = substring(home_office_stats_code,1, position('/' in home_office_stats_code) - 1)::INTEGER
WHERE home_office_stats_code like '%/';

UPDATE offence set sub_category = substring(home_office_stats_code, position('/' in home_office_stats_code) + 1)::INTEGER
WHERE home_office_stats_code like '/%';

ALTER TABLE offence DROP COLUMN home_office_stats_code;
