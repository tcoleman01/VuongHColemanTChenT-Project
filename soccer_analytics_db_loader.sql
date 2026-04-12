-- ============================================================
-- Data Load Script - soccer_analytics_db
-- Load order respects foreign key dependencies
-- ============================================================

USE soccer_analytics_db;
SET SQL_SAFE_UPDATES = 0;
SET SESSION sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
-- ------------------------------------------------------------
-- 1. Country  (no FK dependencies)
-- ------------------------------------------------------------
DELETE FROM Country;
LOAD DATA LOCAL INFILE 'C:/Users/ctr20/Documents/dbdata/Country.csv'
INTO TABLE Country
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(country_abbr, country_name);

select * from Country;
-- ------------------------------------------------------------
-- 2. League  (no FK dependencies)
-- ------------------------------------------------------------
DELETE FROM League;
LOAD DATA LOCAL INFILE 'C:/Users/ctr20/Documents/dbdata/League.csv'
INTO TABLE League
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(league_id, league_name, season_name, @country_abbr)
SET country_abbr = NULLIF(@country_abbr, '');

select * from league;
-- ------------------------------------------------------------
-- 3. Stadium  (no FK dependencies)
-- ------------------------------------------------------------
DELETE FROM Stadium;
LOAD DATA LOCAL INFILE 'C:/Users/ctr20/Documents/dbdata/Stadium.csv'
INTO TABLE Stadium
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(stadium_id, stadium_name, capacity, street_number, street_name, city, @country, @country_abbr, phone_number)
SET country_abbr = NULLIF(@country_abbr, '');

select * from Stadium;
-- ------------------------------------------------------------
-- 4. Position  (no FK dependencies)
-- ------------------------------------------------------------
DELETE FROM `Position`;
LOAD DATA LOCAL INFILE 'C:/Users/ctr20/Documents/dbdata/Position.csv'
INTO TABLE `Position`
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(position_id, position_name, position_category);

select * from Position;
-- ------------------------------------------------------------
-- 5. Coach (depends on Country only — Club rows not needed yet)
-- ------------------------------------------------------------
DELETE FROM Coach;
LOAD DATA LOCAL INFILE 'C:/Users/ctr20/Documents/dbdata/Coach.csv'
INTO TABLE Coach
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(coach_id, first_name, last_name, dob, nationality, @club_id)
SET club_id = NULLIF(@club_id, '');

select * from Coach;
-- ------------------------------------------------------------
-- 6. Club (depends on Country, Stadium, Coach)
-- ------------------------------------------------------------
DELETE FROM Club;
LOAD DATA LOCAL INFILE 'C:/Users/ctr20/Documents/dbdata/Club.csv'
INTO TABLE Club
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(club_id, club_name, country_abbr, @league_id, @stadium_id, @coach_id)
SET league_id  = NULLIF(@league_id, ''),
    stadium_id = NULLIF(@stadium_id, ''),
    coach_id   = NULLIF(@coach_id, '');

select * from club;

-- ------------------------------------------------------------
-- 7. Player  (depends on Position, Country, Club)
-- ------------------------------------------------------------
DELETE FROM Player;
LOAD DATA LOCAL INFILE 'C:/Users/ctr20/Documents/dbdata/Player.csv'
INTO TABLE Player
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(player_id, first_name, last_name, dob, place_of_birth, height_cm, preferred_foot,
 position_id, country_abbr, club_id);

select * from player;

-- ------------------------------------------------------------
-- 8. MarketValue  (depends on Player)
-- ------------------------------------------------------------
DELETE FROM MarketValue;
LOAD DATA LOCAL INFILE 'C:/Users/ctr20/Documents/dbdata/MarketValue.csv'
INTO TABLE MarketValue
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(player_id, @market_value_date, market_value)
SET market_value_date = STR_TO_DATE(@market_value_date, '%b %e, %Y');

select * from MarketValue;
select count(*) from MarketValue;
-- ------------------------------------------------------------
-- 9. Match  (depends on League, Club x2)
-- ------------------------------------------------------------
DELETE FROM `Match`;
LOAD DATA LOCAL INFILE 'C:/Users/ctr20/Documents/dbdata/Match.csv'
INTO TABLE `Match`
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(match_id, home_team_id, away_team_id, match_date, @home_score, @away_score, @home_result, @away_result, @league_id)
SET home_score   = NULLIF(@home_score, ''),
    away_score   = NULLIF(@away_score, ''),
    home_result  = NULLIF(@home_result, ''),
    away_result  = NULLIF(@away_result, ''),
    league_id    = NULLIF(@league_id, '');
select * from `Match`;
select count(*) from `Match`;
-- ------------------------------------------------------------
-- 10. Transfer  (depends on Player, Club x2)
-- ------------------------------------------------------------
DELETE FROM Transfer;
LOAD DATA LOCAL INFILE 'C:/Users/ctr20/Documents/dbdata/Transfer.csv'
INTO TABLE Transfer
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(transfer_id, player_id, old_club_id, new_club_id, transfer_date, transfer_fee);

select * from `Transfer`;
select count(*) from `Transfer`;

-- ------------------------------------------------------------
-- 11. SeasonPerformance  (depends on Player, League)
-- ------------------------------------------------------------
DELETE FROM SeasonPerformance;
LOAD DATA LOCAL INFILE 'C:/Users/ctr20/Documents/dbdata/SeasonPerformance.csv'
INTO TABLE SeasonPerformance
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(player_id, league_id, @appearance_count, @goal_count, @assist_count, @play_time)
SET appearance_count = NULLIF(@appearance_count, ''),
    goal_count       = NULLIF(@goal_count, ''),
    assist_count     = NULLIF(@assist_count, ''),
    play_time        = NULLIF(@play_time, '');

select * from `SeasonPerformance`;
select count(*) from `SeasonPerformance`;

-- ------------------------------------------------------------
-- 12. MatchPerformance  (depends on Match, Player)
-- ------------------------------------------------------------
DELETE FROM MatchPerformance;
LOAD DATA LOCAL INFILE 'C:/Users/ctr20/Documents/dbdata/MatchPerformance.csv'
INTO TABLE MatchPerformance
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(match_id, player_id, play_time, performance_rating);

select * from `MatchPerformance`;

-- ------------------------------------------------------------
-- 13. User  (no FK dependencies)
-- ------------------------------------------------------------
DELETE FROM user;
LOAD DATA LOCAL INFILE 'C:/Users/ctr20/Documents/dbdata/user.csv'
INTO TABLE user
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES
(is_admin, username, password);

select * from `user`;