-- potentially link all new PCSC (life) inchoate offences to nomis, following on from this script a toggle job will be enabled manually via the UI to synchronise the potential PCSC offences with NOMIS
INSERT INTO offence_to_sync_with_nomis
(offence_code, nomis_sync_type, nomis_schedule_name, created_date)
SELECT o2.code, 'LINK_SCHEDULE_TO_OFFENCE', 'POTENTIAL_LINK_PCSC', NOW()
FROM offence o
         JOIN offence o2 ON o2.code LIKE o.code || '%'
WHERE o.max_period_is_life = true
  AND o2.max_period_is_life != true
  AND length(o2.code) > length(o.code);

-- set max period is life to all inchoate offences whose parents have it set
UPDATE offence oupdate
SET max_period_is_life = true
WHERE EXISTS (
    SELECT 1
    FROM offence o
             JOIN offence o2 ON o2.code LIKE o.code || '%'
    WHERE o2.code = oupdate.code
      AND o.max_period_is_life = true
      AND o2.max_period_is_life != true
      AND length(o2.code) > length(o.code)
);

-- Add Encouragement Offence for CE79186
INSERT INTO offence
(code, description, revision_id, cjs_title, start_date, end_date, changed_date, created_date, last_updated_date, category, sub_category, acts_and_sections, parent_offence_id, offence_type, max_period_is_life, max_period_of_indictment_years, max_period_of_indictment_weeks, max_period_of_indictment_days, max_period_of_indictment_months, sdrs_cache)
SELECT
    'CE79186E',  -- Replace 'NEW_CODE' with the new offence code you want to insert
    'Encouragement to ' || description,
    revision_id,
    'Encouragement to ' || cjs_title,
    start_date,
    end_date,
    changed_date,
    created_date,
    NOW(),
    category,
    sub_category,
    acts_and_sections,
    parent_offence_id,
    offence_type,
    max_period_is_life,
    max_period_of_indictment_years,
    max_period_of_indictment_weeks,
    max_period_of_indictment_days,
    max_period_of_indictment_months,
    sdrs_cache
FROM offence
WHERE code = 'CE79186';
