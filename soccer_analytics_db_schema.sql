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
DROP TABLE IF EXISTS League;


CREATE TABLE League (
    league_id       INT             NOT NULL AUTO_INCREMENT,
    league_name     VARCHAR(100)    NOT NULL,
    season_name     VARCHAR(50)     NOT NULL,
    country_abbr    CHAR(3),
    CONSTRAINT pk_league            PRIMARY KEY (league_id),
    CONSTRAINT pak_league           UNIQUE (league_name, season_name),
    CONSTRAINT fk_league_country    FOREIGN KEY (country_abbr)
                                    REFERENCES Country(country_abbr)
                                    ON UPDATE CASCADE ON DELETE SET NULL
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
    country_abbr    CHAR(3),
    phone_number    VARCHAR(25),
    CONSTRAINT pk_stadium           PRIMARY KEY (stadium_id),
    CONSTRAINT fk_stadium_country   FOREIGN KEY (country_abbr)
                                    REFERENCES Country(country_abbr)
                                    ON UPDATE CASCADE ON DELETE SET NULL
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
    league_id       INT,
    stadium_id      INT,
    coach_id        INT,
    CONSTRAINT pk_club              PRIMARY KEY (club_id),
    CONSTRAINT fk_club_country      FOREIGN KEY (country_abbr)
                                    REFERENCES Country(country_abbr)
                                    ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_club_league       FOREIGN KEY (league_id)
                                    REFERENCES League(league_id)
                                    ON UPDATE CASCADE ON DELETE SET NULL,
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
    league_id		INT,
    home_team_id    INT             NOT NULL,
    away_team_id    INT             NOT NULL,
    match_date      DATE            NOT NULL,
    home_score      INT             ,
    away_score      INT             ,
    home_result     ENUM('Win', 'Loss', 'Draw'),
    away_result     ENUM('Win', 'Loss', 'Draw'),
    CONSTRAINT pk_match             PRIMARY KEY (match_id),
    CONSTRAINT pak_match            UNIQUE (home_team_id, away_team_id, match_date),
	CONSTRAINT fk_match_league      FOREIGN KEY (league_id)
                                    REFERENCES league(league_id)
                                    ON UPDATE CASCADE ON DELETE RESTRICT,
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

CREATE TABLE user (
    is_admin BOOL NOT NULL,
    username VARCHAR(20) PRIMARY KEY,
    password VARCHAR(40) NOT NULL
);
