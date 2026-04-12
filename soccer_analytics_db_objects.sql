-- ============================================================
-- Database Programming Objects (Functions, Procedures, Triggers, Events)
-- ============================================================
USE soccer_analytics_db;

DELIMITER //

-- Compute player age from DOB
CREATE FUNCTION fn_player_age(p_dob DATE)
RETURNS INT
DETERMINISTIC
BEGIN
    IF p_dob IS NULL THEN
        RETURN NULL;
    END IF;
    RETURN TIMESTAMPDIFF(YEAR, p_dob, CURDATE());
END//

-- Record a transfer and update the player's current club
CREATE PROCEDURE sp_record_transfer(
    IN p_player_id INT,
    IN p_new_club_id INT,
    IN p_transfer_date DATE,
    IN p_transfer_fee DECIMAL(15,2)
)
BEGIN
    DECLARE v_old_club_id INT;

    SELECT club_id INTO v_old_club_id
    FROM Player
    WHERE player_id = p_player_id;

    INSERT INTO Transfer (
        player_id,
        old_club_id,
        new_club_id,
        transfer_date,
        transfer_fee
    ) VALUES (
        p_player_id,
        v_old_club_id,
        p_new_club_id,
        p_transfer_date,
        p_transfer_fee
    );

    UPDATE Player
    SET club_id = p_new_club_id
    WHERE player_id = p_player_id;
END//

-- Enforce that a team cannot play itself and auto-set match results on insert
CREATE TRIGGER trg_match_validate_insert
BEFORE INSERT ON `Match`
FOR EACH ROW
BEGIN
    IF NEW.home_team_id = NEW.away_team_id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Home and away teams must be different';
    END IF;

    IF NEW.home_score > NEW.away_score THEN
        SET NEW.home_result = 'Win';
        SET NEW.away_result = 'Loss';
    ELSEIF NEW.home_score < NEW.away_score THEN
        SET NEW.home_result = 'Loss';
        SET NEW.away_result = 'Win';
    ELSE
        SET NEW.home_result = 'Draw';
        SET NEW.away_result = 'Draw';
    END IF;
END//

-- Enforce validation and keep results in sync on updates
CREATE TRIGGER trg_match_validate_update
BEFORE UPDATE ON `Match`
FOR EACH ROW
BEGIN
    IF NEW.home_team_id = NEW.away_team_id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Home and away teams must be different';
    END IF;

    IF NEW.home_score > NEW.away_score THEN
        SET NEW.home_result = 'Win';
        SET NEW.away_result = 'Loss';
    ELSEIF NEW.home_score < NEW.away_score THEN
        SET NEW.home_result = 'Loss';
        SET NEW.away_result = 'Win';
    ELSE
        SET NEW.home_result = 'Draw';
        SET NEW.away_result = 'Draw';
    END IF;
END//

-- Nightly event to backfill results if scores exist but results are NULL
CREATE EVENT ev_update_match_results
ON SCHEDULE EVERY 1 DAY
DO
    UPDATE `Match`
    SET home_result = CASE
            WHEN home_score > away_score THEN 'Win'
            WHEN home_score < away_score THEN 'Loss'
            ELSE 'Draw'
        END,
        away_result = CASE
            WHEN home_score > away_score THEN 'Loss'
            WHEN home_score < away_score THEN 'Win'
            ELSE 'Draw'
        END
    WHERE home_result IS NULL OR away_result IS NULL//

DELIMITER ;
