USE soccer_analytics_db;

-- ============================================================
-- Football Analytics Database - MySQL Schema
-- ============================================================

DROP DATABASE IF EXISTS soccer_analytics_db;
CREATE DATABASE soccer_analytics_db;
USE soccer_analytics_db;

-- ------------------------------------------------------------
-- Country
-- ------------------------------------------------------------
CREATE TABLE Country (
    country_abbr    CHAR(3)         NOT NULL,
    country_name    VARCHAR(100)    NOT NULL,
    CONSTRAINT pk_country       PRIMARY KEY (country_abbr),
    CONSTRAINT ak_country_name  UNIQUE (country_name)
);

-- ------------------------------------------------------------
-- League
-- ------------------------------------------------------------
CREATE TABLE League (
    league_id       INT             NOT NULL AUTO_INCREMENT,
    league_name     VARCHAR(100)    NOT NULL,
    season_name     VARCHAR(50)     NOT NULL,
    country_abbr    CHAR(3)         NOT NULL,
    CONSTRAINT pk_league            PRIMARY KEY (league_id),
    CONSTRAINT pak_league           UNIQUE (league_name, season_name),
    CONSTRAINT fk_league_country    FOREIGN KEY (country_abbr)
                                    REFERENCES Country(country_abbr)
                                    ON UPDATE CASCADE ON DELETE RESTRICT
);

-- ------------------------------------------------------------
-- Stadium
-- ------------------------------------------------------------
CREATE TABLE Stadium (
    stadium_id      INT             NOT NULL AUTO_INCREMENT,
    stadium_name    VARCHAR(100)    NOT NULL,
    capacity        INT,
    street_number   VARCHAR(20),
    street_name     VARCHAR(100),
    city            VARCHAR(100),
    country         VARCHAR(100),
    phone_number    VARCHAR(25),
    CONSTRAINT pk_stadium PRIMARY KEY (stadium_id)
);

-- ------------------------------------------------------------
-- Club
-- coach_id FK is added via ALTER TABLE below to avoid
-- circular dependency with Coach
-- ------------------------------------------------------------
CREATE TABLE Club (
    club_id         INT             NOT NULL AUTO_INCREMENT,
    club_name       VARCHAR(100)    NOT NULL,
    country_abbr    CHAR(3)         NOT NULL,
    league_id       INT             NOT NULL,
    stadium_id      INT,
    coach_id        INT,
    CONSTRAINT pk_club              PRIMARY KEY (club_id),
    CONSTRAINT fk_club_country      FOREIGN KEY (country_abbr)
                                    REFERENCES Country(country_abbr)
                                    ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_club_league       FOREIGN KEY (league_id)
                                    REFERENCES League(league_id)
                                    ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_club_stadium      FOREIGN KEY (stadium_id)
                                    REFERENCES Stadium(stadium_id)
                                    ON UPDATE CASCADE ON DELETE SET NULL
);

-- ------------------------------------------------------------
-- Coach
-- nationality FK -> Country
-- club_id FK -> Club added via ALTER TABLE below
-- ------------------------------------------------------------
CREATE TABLE Coach (
    coach_id        INT             NOT NULL AUTO_INCREMENT,
    first_name      VARCHAR(50)     NOT NULL,
    last_name       VARCHAR(50)     NOT NULL,
    dob             DATE,
    nationality     CHAR(3),
    club_id         INT,
    CONSTRAINT pk_coach             PRIMARY KEY (coach_id),
    CONSTRAINT fk_coach_country     FOREIGN KEY (nationality)
                                    REFERENCES Country(country_abbr)
                                    ON UPDATE CASCADE ON DELETE SET NULL
);

-- Resolve Club <-> Coach circular dependency this is the 1.1 rls
ALTER TABLE Club
    ADD CONSTRAINT fk_club_coach
    FOREIGN KEY (coach_id) REFERENCES Coach(coach_id)
    ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE Coach
    ADD CONSTRAINT fk_coach_club
    FOREIGN KEY (club_id) REFERENCES Club(club_id)
    ON UPDATE CASCADE ON DELETE SET NULL;

-- ------------------------------------------------------------
-- Position
-- ------------------------------------------------------------
CREATE TABLE `Position` (
    position_id         INT             NOT NULL AUTO_INCREMENT,
    position_name       VARCHAR(50)     NOT NULL,
    position_category   VARCHAR(50),
    CONSTRAINT pk_position PRIMARY KEY (position_id)
);

-- ------------------------------------------------------------
-- Player
-- ------------------------------------------------------------
CREATE TABLE Player (
    player_id       INT             NOT NULL AUTO_INCREMENT,
    first_name      VARCHAR(50)     NOT NULL,
    last_name       VARCHAR(50)     NOT NULL,
    dob             DATE,
    place_of_birth  VARCHAR(100),
    height_cm       DECIMAL(5,2),
    preferred_foot  ENUM('Left', 'Right', 'Both'),
    position_id     INT,
    country_abbr    CHAR(3),
    club_id         INT,
    CONSTRAINT pk_player            PRIMARY KEY (player_id),
    CONSTRAINT fk_player_position   FOREIGN KEY (position_id)
                                    REFERENCES `Position`(position_id)
                                    ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_player_country    FOREIGN KEY (country_abbr)
                                    REFERENCES Country(country_abbr)
                                    ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_player_club       FOREIGN KEY (club_id)
                                    REFERENCES Club(club_id)
                                    ON UPDATE CASCADE ON DELETE SET NULL
);

-- ------------------------------------------------------------
-- MarketValue  (weak entity - no identity without Player)
-- ------------------------------------------------------------
CREATE TABLE MarketValue (
    player_id           INT             NOT NULL,
    market_value_date   DATE            NOT NULL,
    market_value        DECIMAL(15,2)   NOT NULL,
    CONSTRAINT pk_marketvalue   PRIMARY KEY (player_id, market_value_date),
    CONSTRAINT fk_mv_player     FOREIGN KEY (player_id)
                                REFERENCES Player(player_id)
                                ON UPDATE CASCADE ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- Match
-- PK: match_id
-- PAK: (home_team_id, away_team_id, match_date)
-- CHECK ensures a team can't play itself
-- ------------------------------------------------------------
CREATE TABLE `Match` (
    match_id        INT             NOT NULL AUTO_INCREMENT,
    home_team_id    INT             NOT NULL,
    away_team_id    INT             NOT NULL,
    match_date      DATE            NOT NULL,
    home_score      INT             DEFAULT 0,
    away_score      INT             DEFAULT 0,
    home_result     ENUM('Win', 'Loss', 'Draw'),
    away_result     ENUM('Win', 'Loss', 'Draw'),
    CONSTRAINT pk_match             PRIMARY KEY (match_id),
    CONSTRAINT pak_match            UNIQUE (home_team_id, away_team_id, match_date),
    CONSTRAINT fk_match_home        FOREIGN KEY (home_team_id)
                                    REFERENCES Club(club_id)
                                    ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_match_away        FOREIGN KEY (away_team_id)
                                    REFERENCES Club(club_id)
                                    ON UPDATE CASCADE ON DELETE RESTRICT
);

-- ------------------------------------------------------------
-- Transfer  (complex relationship: Player + OldClub + NewClub)
-- old_club_id nullable - player may have no prior club
-- ------------------------------------------------------------
CREATE TABLE Transfer (
    transfer_id     INT             NOT NULL AUTO_INCREMENT,
    player_id       INT             NOT NULL,
    old_club_id     INT,
    new_club_id     INT             NOT NULL,
    transfer_date   DATE            NOT NULL,
    transfer_fee    DECIMAL(15,2),
    CONSTRAINT pk_transfer          PRIMARY KEY (transfer_id),
    CONSTRAINT fk_transfer_player   FOREIGN KEY (player_id)
                                    REFERENCES Player(player_id)
                                    ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_transfer_oldclub  FOREIGN KEY (old_club_id)
                                    REFERENCES Club(club_id)
                                    ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_transfer_newclub  FOREIGN KEY (new_club_id)
                                    REFERENCES Club(club_id)
                                    ON UPDATE CASCADE ON DELETE RESTRICT
);

-- ------------------------------------------------------------
-- SeasonPerformance  (Player stats per League/Season)
-- ------------------------------------------------------------
CREATE TABLE SeasonPerformance (
    player_id           INT     NOT NULL,
    league_id           INT     NOT NULL,
    appearance_count    INT     DEFAULT 0,
    goal_count          INT     DEFAULT 0,
    assist_count        INT     DEFAULT 0,
    play_time           INT     DEFAULT 0,   -- minutes
    CONSTRAINT pk_seasonperf    PRIMARY KEY (player_id, league_id),
    CONSTRAINT fk_sp_player     FOREIGN KEY (player_id)
                                REFERENCES Player(player_id)
                                ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_sp_league     FOREIGN KEY (league_id)
                                REFERENCES League(league_id)
                                ON UPDATE CASCADE ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- MatchPerformance  (Player stats per Match)
-- Relationship attributes: play_time, performance_rating
-- ------------------------------------------------------------
CREATE TABLE MatchPerformance (
    match_id            INT             NOT NULL,
    player_id           INT             NOT NULL,
    play_time           INT             DEFAULT 0,
    performance_rating  DECIMAL(4,2),
    CONSTRAINT pk_matchperf     PRIMARY KEY (match_id, player_id),
    CONSTRAINT fk_mp_match      FOREIGN KEY (match_id)
                                REFERENCES `Match`(match_id)
                                ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_mp_player     FOREIGN KEY (player_id)
                                REFERENCES Player(player_id)
                                ON UPDATE CASCADE ON DELETE CASCADE
);

-- load excel into table, market_value.csv
DROP TABLE IF EXISTS staging_market_value;

CREATE TABLE staging_market_value (
    row_num         INT,
    first_name      VARCHAR(50),
    last_name       VARCHAR(50),
    mv_date         VARCHAR(20),
    mv_club         VARCHAR(100),
    mv_unit         VARCHAR(10),
    mv_value        DECIMAL(15,2)
);

LOAD DATA LOCAL INFILE 'C:/Users/Owner/Desktop/CS5200Local/market_value.csv'
INTO TABLE staging_market_value
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(row_num, first_name, last_name, mv_date, mv_club, mv_unit, mv_value);

-- CHECKING TO SEE IF WORKED IMPORTS
SELECT COUNT(*) AS total_staging_rows
FROM staging_market_value;

SELECT *
FROM staging_market_value
LIMIT 10;


-- player 2 staging 
DROP TABLE IF EXISTS staging_player;

CREATE TABLE staging_player (
    row_num              INT,
    name_in_home_country VARCHAR(150),
    dob                  VARCHAR(20),
    place_of_birth       VARCHAR(100),
    height               DECIMAL(5,2),
    position             VARCHAR(100),
    foot                 VARCHAR(20),
    club                 VARCHAR(100),
    join_date            VARCHAR(20),
    contract_expires     VARCHAR(20),
    first_name           VARCHAR(50),
    last_name            VARCHAR(50),
    citizenship1         VARCHAR(100),
    citizenship2         VARCHAR(100)
);

LOAD DATA LOCAL INFILE 'C:/Users/Owner/Desktop/CS5200Local/player2.csv'
INTO TABLE staging_player
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(@dummy, name_in_home_country, dob, place_of_birth, height, position, foot, club, join_date, contract_expires, first_name, last_name, citizenship1, citizenship2);

-- CHECKING TO SEE IF IMPORT CORRECTLY 
SELECT COUNT(*) AS total_rows
FROM staging_player;

SELECT *
FROM staging_player
LIMIT 10;

-- ============================================================
-- Database Programming Objects (Functions, Procedures, Triggers, Events)
-- ============================================================

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
