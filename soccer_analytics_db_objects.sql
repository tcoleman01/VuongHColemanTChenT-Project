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












-- User view operations

-- ============================================================
-- DESCRIPTION
-- Function: fn_player_age
-- Purpose: Computes a player's current age from their date of birth
-- Input: p_dob (DATE) - player's date of birth
-- Output: INT - age in years, NULL if dob is NULL
-- ============================================================
DROP FUNCTION IF EXISTS fn_player_age;
DELIMITER //
CREATE FUNCTION fn_player_age(p_dob DATE)
RETURNS INT
DETERMINISTIC
BEGIN
    IF p_dob IS NULL THEN
        RETURN NULL;
    END IF;
    RETURN TIMESTAMPDIFF(YEAR, p_dob, CURDATE());
END//
DELIMITER ;


-- ============================================================
-- DESCRIPTION
-- Procedure: sp_players_in_league
-- Purpose: Retrieves all players who played in a specified league and season
-- Inputs: p_league_name (VARCHAR) - e.g. 'Premier League'
--         p_season_name (VARCHAR) - e.g. '21/22'
-- Output: player_id, first_name, last_name, age, position, position_category, preferred_foot, club_name
-- ============================================================
DROP PROCEDURE IF EXISTS sp_players_in_league;
DELIMITER //
CREATE PROCEDURE sp_players_in_league(
    IN p_league_name VARCHAR(100),
    IN p_season_name VARCHAR(50)
)
BEGIN
    SELECT 
        p.player_id,
        p.first_name,
        p.last_name,
        fn_player_age(p.dob) AS age,
        pos.position_name,
        pos.position_category,
        p.preferred_foot,
        c.club_name
    FROM Player p
    LEFT JOIN `Position` pos ON p.position_id = pos.position_id
    LEFT JOIN Club c ON p.club_id = c.club_id
    JOIN SeasonPerformance sp ON p.player_id = sp.player_id
    JOIN League l ON sp.league_id = l.league_id
    WHERE l.league_name = p_league_name
      AND l.season_name = p_season_name
    ORDER BY c.club_name, p.last_name;
END//
DELIMITER ;
-- test
CALL sp_players_in_league('Premier League', '22/23');

-- see which league and season have most player for testing
SELECT DISTINCT league_name, season_name, COUNT(*) as player_count
FROM SeasonPerformance sp
JOIN League l ON sp.league_id = l.league_id
GROUP BY league_name, season_name
ORDER BY player_count DESC
LIMIT 10;



-- ============================================================
-- DESCRIPTION
-- Procedure: sp_player_stats
-- Purpose: Retrieves season performance statistics for a specific player
-- Inputs: p_first_name (VARCHAR) - player's first name
--         p_last_name (VARCHAR) - player's last name
-- Output: first_name, last_name, league_name, season_name, 
--         appearance_count, goal_count, assist_count, play_time
-- ============================================================
DROP PROCEDURE IF EXISTS sp_player_stats;
DELIMITER //
CREATE PROCEDURE sp_player_stats(
    IN p_first_name VARCHAR(50),
    IN p_last_name VARCHAR(50)
)
BEGIN
    SELECT
        p.first_name,
        p.last_name,
        l.league_name,
        l.season_name,
        sp.appearance_count,
        sp.goal_count,
        sp.assist_count,
        sp.play_time
    FROM SeasonPerformance sp
    JOIN Player p ON sp.player_id = p.player_id
    JOIN League l ON sp.league_id = l.league_id
    WHERE p.first_name = p_first_name
      AND p.last_name = p_last_name
    ORDER BY l.season_name DESC;
END//
DELIMITER ;

CALL sp_player_stats('Toni', 'Kroos');


-- ============================================================
-- DESCRIPTION
-- Procedure: sp_top_scorers
-- Purpose: Retrieves top scorers for a specific league and season
-- Inputs: p_league_name (VARCHAR) - e.g. 'Premier League'
--         p_season_name (VARCHAR) - e.g. '22/23'
--         p_limit (INT) - number of results to return e.g. 10
-- Output: first_name, last_name, club_name, goal_count, assist_count, appearance_count
-- ============================================================
DROP PROCEDURE IF EXISTS sp_top_scorers;
DELIMITER //
CREATE PROCEDURE sp_top_scorers(
    IN p_league_name VARCHAR(100),
    IN p_season_name VARCHAR(50),
    IN p_limit INT
)
BEGIN
    SELECT
        p.first_name,
        p.last_name,
        c.club_name,
        sp.goal_count,
        sp.assist_count,
        sp.appearance_count
    FROM SeasonPerformance sp
    JOIN Player p ON sp.player_id = p.player_id
    JOIN League l ON sp.league_id = l.league_id
    LEFT JOIN Club c ON p.club_id = c.club_id
    WHERE l.league_name = p_league_name
      AND l.season_name = p_season_name
    ORDER BY sp.goal_count DESC
    LIMIT p_limit;
END//
DELIMITER ;
-- testing
CALL sp_top_scorers('Premier League', '22/23', 10);


-- ============================================================
-- DESCRIPTION
-- Procedure: sp_teams_in_league
-- Purpose: Retrieves all clubs participating in a specific league and season
-- Inputs: p_league_name (VARCHAR) - e.g. 'Premier League'
--         p_season_name (VARCHAR) - e.g. '22/23'
-- Output: club_id, club_name, country_abbr
-- ============================================================
DROP PROCEDURE IF EXISTS sp_teams_in_league;
DELIMITER //
CREATE PROCEDURE sp_teams_in_league(
    IN p_league_name VARCHAR(100),
    IN p_season_name VARCHAR(50)
)
BEGIN
    SELECT DISTINCT
        c.club_id,
        c.club_name,
        c.country_abbr
    FROM Club c
    JOIN Player p ON p.club_id = c.club_id
    JOIN SeasonPerformance sp ON sp.player_id = p.player_id
    JOIN League l ON sp.league_id = l.league_id
    WHERE l.league_name = p_league_name
      AND l.season_name = p_season_name
    ORDER BY c.club_name;
END//
DELIMITER ;
-- testing
CALL sp_teams_in_league('Premier League', '22/23');

-- ============================================================
-- DESCRIPTION
-- Procedure: sp_match_results
-- Purpose: Retrieves match results for a specific league and season
-- Inputs: p_league_name (VARCHAR) - e.g. 'Premier League'
--         p_season_name (VARCHAR) - e.g. '22/23'
-- Output: match_id, match_date, home_team, home_score, away_team, away_score, home_result, away_result
-- ============================================================
DROP PROCEDURE IF EXISTS sp_match_results;
DELIMITER //
CREATE PROCEDURE sp_match_results(
    IN p_league_name VARCHAR(100),
    IN p_season_name VARCHAR(50)
)
BEGIN
    SELECT
        m.match_id,
        m.match_date,
        hc.club_name AS home_team,
        m.home_score,
        ac.club_name AS away_team,
        m.away_score,
        m.home_result,
        m.away_result
    FROM `Match` m
    JOIN Club hc ON m.home_team_id = hc.club_id
    JOIN Club ac ON m.away_team_id = ac.club_id
    JOIN League l ON m.league_id = l.league_id
    WHERE l.league_name = p_league_name
      AND l.season_name = p_season_name
    ORDER BY m.match_date;
END//
DELIMITER ;

-- testing
-- only 2 league/season in csv 
CALL sp_match_results('Premier League', '25/26');  
CALL sp_match_results('Champions League', '25/26');


-- ============================================================
-- DESCRIPTION
-- Procedure: sp_coach_stats
-- Purpose: Retrieves coach information and their current club
-- Inputs: p_first_name (VARCHAR) - coach's first name
--         p_last_name (VARCHAR) - coach's last name
-- Output: coach_id, first_name, last_name, dob, age, nationality, club_name
-- ============================================================
DROP PROCEDURE IF EXISTS sp_coach_stats;
DELIMITER //
CREATE PROCEDURE sp_coach_stats(
    IN p_first_name VARCHAR(50),
    IN p_last_name VARCHAR(50)
)
BEGIN
    SELECT
        co.coach_id,
        co.first_name,
        co.last_name,
        co.dob,
        fn_player_age(co.dob) AS age,
        co.nationality,
        c.club_name AS current_club
    FROM Coach co
    LEFT JOIN Club c ON co.club_id = c.club_id
    WHERE co.first_name = p_first_name
      AND co.last_name = p_last_name;
END//
DELIMITER ;

-- testin
CALL sp_coach_stats('Pep', 'Guardiola');


-- ============================================================
-- DESCRIPTION
-- Procedure: sp_stadium_stats
-- Purpose: Retrieves stadium information and its home club
-- Inputs: p_stadium_name (VARCHAR) - stadium name or partial name
-- Output: stadium_id, stadium_name, capacity, city, country, phone_number, home_club
-- ============================================================
DROP PROCEDURE IF EXISTS sp_stadium_stats;
DELIMITER //
CREATE PROCEDURE sp_stadium_stats(
    IN p_stadium_name VARCHAR(100)
)
BEGIN
    SELECT
        s.stadium_id,
        s.stadium_name,
        s.capacity,
        s.city,
        s.country,
        s.phone_number,
        c.club_name AS home_club
    FROM Stadium s
    LEFT JOIN Club c ON c.stadium_id = s.stadium_id
    WHERE s.stadium_name LIKE CONCAT('%', p_stadium_name, '%');
END//
DELIMITER ;
-- testin
CALL sp_stadium_stats('Old Trafford');



-- ============================================================
-- DESCRIPTION
-- Procedure: sp_player_market_value
-- Purpose: Retrieves full market value history for a specific player
-- Inputs: p_first_name (VARCHAR) - player's first name
--         p_last_name (VARCHAR) - player's last name
-- Output: first_name, last_name, market_value_date, market_value
-- ============================================================
DROP PROCEDURE IF EXISTS sp_player_market_value;
DELIMITER //
CREATE PROCEDURE sp_player_market_value(
    IN p_first_name VARCHAR(50),
    IN p_last_name VARCHAR(50)
)
BEGIN
    SELECT
        p.first_name,
        p.last_name,
        mv.market_value_date,
        mv.market_value
    FROM MarketValue mv
    JOIN Player p ON mv.player_id = p.player_id
    WHERE p.first_name = p_first_name
      AND p.last_name = p_last_name
    ORDER BY mv.market_value_date DESC;
END//
DELIMITER 

--  testing
CALL sp_player_market_value('Toni', 'Kroos');


-- ============================================================
-- DESCRIPTION
-- Procedure: sp_player_transfers
-- Purpose: Retrieves full transfer history for a specific player
-- Inputs: p_first_name (VARCHAR) - player's first name
--         p_last_name (VARCHAR) - player's last name
-- Output: first_name, last_name, from_club, to_club, transfer_date, transfer_fee
-- ============================================================
DROP PROCEDURE IF EXISTS sp_player_transfers;
DELIMITER //
CREATE PROCEDURE sp_player_transfers(
    IN p_first_name VARCHAR(50),
    IN p_last_name VARCHAR(50)
)
BEGIN
    SELECT
        p.first_name,
        p.last_name,
        oc.club_name AS from_club,
        nc.club_name AS to_club,
        t.transfer_date,
        t.transfer_fee
    FROM Transfer t
    JOIN Player p ON t.player_id = p.player_id
    LEFT JOIN Club oc ON t.old_club_id = oc.club_id
    JOIN Club nc ON t.new_club_id = nc.club_id
    WHERE p.first_name = p_first_name
      AND p.last_name = p_last_name
    ORDER BY t.transfer_date DESC;
END//
DELIMITER ;

-- testin
CALL sp_player_transfers('Zlatan', 'Ibrahimović');