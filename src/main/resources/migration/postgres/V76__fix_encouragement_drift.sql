-- Encouragement ('E' suffix) offences are minted locally: there is no SDRS source for them.
-- They were created as a point-in-time copy of the parent (V55/V56/V60, and
-- AdminService.createEncouragementOffence) and nothing has updated them since, so they drift
-- whenever the parent is updated by the SDRS delta load or the HO code load.

-- ---------------------------------------------------------------------------
-- Derive encouragement language
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION derive_encouragement_legislation(parent_legislation TEXT) RETURNS TEXT AS
$$
DECLARE
    sca_sentence CONSTANT TEXT := 'Also contrary to sections 44 and 58 of the Serious Crimes Act 2007';
    parent_text           TEXT := BTRIM(COALESCE(parent_legislation, ''));
BEGIN
    IF parent_text LIKE '%' || sca_sentence THEN
        RETURN parent_text;
    END IF;
    IF parent_text = '' THEN
        RETURN sca_sentence;
    END IF;
    RETURN parent_text || ' ' || sca_sentence;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- ---------------------------------------------------------------------------
-- Part 1: backfill historic drift
-- ---------------------------------------------------------------------------

WITH updated AS (
    UPDATE offence c
        SET description                     = 'Encouragement to ' || p.description,
            cjs_title                       = 'Encouragement to ' || p.cjs_title,
            acts_and_sections               = derive_encouragement_legislation(p.acts_and_sections),
            category                        = p.category,
            sub_category                    = p.sub_category,
            offence_type                    = p.offence_type,
            custodial_indicator             = p.custodial_indicator,
            max_period_is_life              = p.max_period_is_life,
            max_period_of_indictment_years  = p.max_period_of_indictment_years,
            max_period_of_indictment_months = p.max_period_of_indictment_months,
            max_period_of_indictment_weeks  = p.max_period_of_indictment_weeks,
            max_period_of_indictment_days   = p.max_period_of_indictment_days,
            revision_id                     = p.revision_id,
            changed_date                    = p.changed_date,
            sdrs_cache                      = p.sdrs_cache,
            last_updated_date               = NOW()
        FROM offence p
        WHERE c.parent_offence_id = p.id
            AND c.code = p.code || 'E'
            AND (
                 c.description, c.cjs_title, c.acts_and_sections, c.category, c.sub_category,
                 c.offence_type, c.custodial_indicator, c.max_period_is_life,
                 c.max_period_of_indictment_years, c.max_period_of_indictment_months,
                 c.max_period_of_indictment_weeks, c.max_period_of_indictment_days,
                 c.revision_id, c.sdrs_cache
                  ) IS DISTINCT FROM (
                                      'Encouragement to ' || p.description, 'Encouragement to ' || p.cjs_title,
                                      derive_encouragement_legislation(p.acts_and_sections),
                                      p.category, p.sub_category, p.offence_type, p.custodial_indicator,
                                      p.max_period_is_life, p.max_period_of_indictment_years,
                                      p.max_period_of_indictment_months, p.max_period_of_indictment_weeks,
                                      p.max_period_of_indictment_days, p.revision_id, p.sdrs_cache
                )
        RETURNING c.code)
INSERT
INTO event_to_raise (event_type, offence_code)
SELECT 'OFFENCE_CHANGED', code
FROM updated;

-- ---------------------------------------------------------------------------
-- Part 2: keep them in step from now on
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION sync_encouragement_offence() RETURNS TRIGGER AS
$$
DECLARE
    child_code TEXT := NEW.code || 'E';
    touched    INT;
BEGIN
    UPDATE offence c
    SET description                     = 'Encouragement to ' || NEW.description,
        cjs_title                       = 'Encouragement to ' || NEW.cjs_title,
        acts_and_sections               = derive_encouragement_legislation(NEW.acts_and_sections),
        end_date                        = CASE
                                              WHEN NEW.end_date IS DISTINCT FROM OLD.end_date THEN NEW.end_date
                                              ELSE c.end_date
            END,
        category                        = NEW.category,
        sub_category                    = NEW.sub_category,
        offence_type                    = NEW.offence_type,
        custodial_indicator             = NEW.custodial_indicator,
        max_period_is_life              = NEW.max_period_is_life,
        max_period_of_indictment_years  = NEW.max_period_of_indictment_years,
        max_period_of_indictment_months = NEW.max_period_of_indictment_months,
        max_period_of_indictment_weeks  = NEW.max_period_of_indictment_weeks,
        max_period_of_indictment_days   = NEW.max_period_of_indictment_days,
        revision_id                     = NEW.revision_id,
        changed_date                    = NEW.changed_date,
        sdrs_cache                      = NEW.sdrs_cache,
        last_updated_date               = NOW()
    WHERE c.parent_offence_id = NEW.id
      AND c.code = child_code;

    GET DIAGNOSTICS touched = ROW_COUNT;

    IF touched > 0 THEN
        INSERT INTO event_to_raise (event_type, offence_code) VALUES ('OFFENCE_CHANGED', child_code);
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER offence_sync_encouragement
    AFTER UPDATE
    ON offence
    FOR EACH ROW
    WHEN (
        LENGTH(NEW.code) < 8
            AND (
                 OLD.description, OLD.cjs_title, OLD.acts_and_sections, OLD.end_date, OLD.category,
                 OLD.sub_category, OLD.offence_type, OLD.custodial_indicator, OLD.max_period_is_life,
                 OLD.max_period_of_indictment_years, OLD.max_period_of_indictment_months,
                 OLD.max_period_of_indictment_weeks, OLD.max_period_of_indictment_days,
                 OLD.revision_id, OLD.sdrs_cache
            ) IS DISTINCT FROM (
                                NEW.description, NEW.cjs_title, NEW.acts_and_sections, NEW.end_date, NEW.category,
                                NEW.sub_category, NEW.offence_type, NEW.custodial_indicator, NEW.max_period_is_life,
                                NEW.max_period_of_indictment_years, NEW.max_period_of_indictment_months,
                                NEW.max_period_of_indictment_weeks, NEW.max_period_of_indictment_days,
                                NEW.revision_id, NEW.sdrs_cache
            )
        )
EXECUTE FUNCTION sync_encouragement_offence();