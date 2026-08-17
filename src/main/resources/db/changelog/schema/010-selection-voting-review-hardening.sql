--liquibase formatted sql
--changeset venomenon328:010-selection-voting-review-hardening splitStatements:false

-- A frozen electorate remains the source of the initial membership even if one
-- of its members is deactivated later. New voluntary joins still require an
-- active participant.
CREATE OR REPLACE FUNCTION validate_challenge_participation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'challenge participation is immutable after joining';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM participant WHERE id = NEW.participant_id AND active)
       AND NOT EXISTS (
            SELECT 1
              FROM selection_electorate electorate
              JOIN generation_attempt attempt ON attempt.challenge_session_id = electorate.challenge_session_id
              JOIN curated_offer_set offer_set ON offer_set.generation_attempt_id = attempt.id
              JOIN curated_offer offer ON offer.curated_offer_set_id = offer_set.id
             WHERE electorate.participant_id = NEW.participant_id
               AND offer.id = (SELECT curated_offer_id FROM challenge WHERE id = NEW.challenge_id)
       ) THEN
        RAISE EXCEPTION 'only active registered participants may join a challenge outside its frozen electorate';
    END IF;
    RETURN NEW;
END;
$$;

-- Apply outcomes are observed outside the short session-lock transaction.
-- Only forward transitions may change the durable state, so a delayed earlier
-- outcome cannot erase a resulting offer-set ID or a terminal result.
CREATE OR REPLACE FUNCTION validate_selection_voting_apply_transition()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.status <> 'COMPLETED' THEN
        RETURN NEW;
    END IF;

    IF NEW.apply_state = OLD.apply_state THEN
        IF NEW.resulting_offer_set_id IS DISTINCT FROM OLD.resulting_offer_set_id
           OR NEW.apply_detail IS DISTINCT FROM OLD.apply_detail
           OR NEW.applied_at IS DISTINCT FROM OLD.applied_at THEN
            RAISE EXCEPTION 'selection voting apply state is immutable without a forward transition';
        END IF;
        RETURN NEW;
    END IF;

    IF OLD.apply_state = 'PENDING'
       AND NEW.apply_state = 'CONFIRMED'
       AND OLD.result_option_type <> 'REROLL'
       AND NEW.resulting_offer_set_id IS NULL
       AND NEW.apply_detail IS NULL THEN
        RETURN NEW;
    END IF;

    IF OLD.result_option_type = 'REROLL'
       AND OLD.apply_state = 'PENDING'
       AND NEW.apply_state = 'REROLL_IN_PROGRESS'
       AND NEW.resulting_offer_set_id IS NULL THEN
        RETURN NEW;
    END IF;

    IF OLD.result_option_type = 'REROLL'
       AND OLD.apply_state IN ('PENDING', 'REROLL_IN_PROGRESS')
       AND NEW.apply_state = 'REROLL_OFFER_READY'
       AND NEW.resulting_offer_set_id IS NOT NULL
       AND NEW.apply_detail IS NULL THEN
        RETURN NEW;
    END IF;

    IF OLD.result_option_type = 'REROLL'
       AND OLD.apply_state IN ('PENDING', 'REROLL_IN_PROGRESS')
       AND NEW.apply_state IN ('REROLL_EXHAUSTED', 'REROLL_FAILED')
       AND NEW.resulting_offer_set_id IS NULL THEN
        RETURN NEW;
    END IF;

    IF OLD.result_option_type = 'REROLL'
       AND OLD.apply_state = 'REROLL_OFFER_READY'
       AND NEW.apply_state = 'REROLL_AUTO_CONFIRM_PENDING'
       AND NEW.resulting_offer_set_id = OLD.resulting_offer_set_id
       AND NEW.apply_detail IS NULL THEN
        RETURN NEW;
    END IF;

    IF OLD.result_option_type = 'REROLL'
       AND OLD.apply_state = 'REROLL_AUTO_CONFIRM_PENDING'
       AND NEW.apply_state = 'REROLL_AUTO_CONFIRMED'
       AND NEW.resulting_offer_set_id = OLD.resulting_offer_set_id
       AND NEW.apply_detail IS NULL THEN
        RETURN NEW;
    END IF;

    RAISE EXCEPTION 'selection voting apply state must advance monotonically';
END;
$$;

CREATE TRIGGER trg_selection_voting_round_apply_transition
BEFORE UPDATE ON selection_voting_round
FOR EACH ROW EXECUTE FUNCTION validate_selection_voting_apply_transition();
