-- Add Encouragement Offence for CJ15001
INSERT INTO offence
(code, description, revision_id, cjs_title, start_date, end_date, changed_date, created_date, last_updated_date, category, sub_category, acts_and_sections, parent_offence_id, offence_type, max_period_is_life, max_period_of_indictment_years, max_period_of_indictment_weeks, max_period_of_indictment_days, max_period_of_indictment_months, sdrs_cache, custodial_indicator)
SELECT
    'CJ15001E',  -- Replace 'NEW_CODE' with the new offence code you want to insert
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
    id,
    offence_type,
    max_period_is_life,
    max_period_of_indictment_years,
    max_period_of_indictment_weeks,
    max_period_of_indictment_days,
    max_period_of_indictment_months,
    sdrs_cache,
    custodial_indicator
FROM offence
WHERE code = 'CJ15001';


-- Update custodial indicators for encouragement offences from the parent.

UPDATE offence
SET custodial_indicator = (SELECT parent.custodial_indicator
                           FROM offence AS parent
                           WHERE parent.id = offence.parent_offence_id)
WHERE offence.custodial_indicator IS NULL AND offence.code LIKE '%E'