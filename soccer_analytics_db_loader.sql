USE soccer_analytics_db;
-- MAKE SURE THIS IS ACTIVE BEFORE IMPORT TABLE
SET GLOBAL local_infile = 1;
-- STAGING AREA 
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

LOAD DATA LOCAL INFILE 'C:/Users/ctr20/Documents/dbdata/market_value.csv'
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

LOAD DATA LOCAL INFILE 'C:/Users/ctr20/Documents/dbdata/player.csv'
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


-- staging player stat 
DROP TABLE IF EXISTS staging_player_stat;

CREATE TABLE staging_player_stat (
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    season VARCHAR(20),
    league VARCHAR(100),
    appear INT,
    goal INT,
    assist INT,
    play_time INT
);

LOAD DATA LOCAL INFILE 'C:/Users/Owner/Desktop/CS5200Local/player_stat.csv'
INTO TABLE staging_player_stat
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(first_name, last_name, season, league, appear, goal, assist, play_time);

-- check if import correctly
SELECT COUNT(*) AS total_rows
FROM staging_player_stat;

SELECT *
FROM staging_player_stat
LIMIT 10;

-- staging transfer history 

DROP TABLE IF EXISTS staging_transfer_history;

CREATE TABLE staging_transfer_history (
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    transfer_date VARCHAR(20),
    from_club VARCHAR(100),
    to_club VARCHAR(100),
    market_value VARCHAR(50),
    fee VARCHAR(50)
);

LOAD DATA LOCAL INFILE 'C:/Users/Owner/Desktop/CS5200Local/transfer_history2.csv'
INTO TABLE staging_transfer_history
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(first_name, last_name, transfer_date, from_club, to_club, market_value, fee);

-- make sure import correct
SELECT COUNT(*) FROM staging_transfer_history;

SELECT * 
FROM staging_transfer_history
LIMIT 10;

-- CONNECT STAGING TO TABLE SCHEMA
-- Position table 
-- clean up if dirty data
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `Position`;
SET FOREIGN_KEY_CHECKS = 1;

-- Position table insert
INSERT INTO `Position` (position_name, position_category)
SELECT DISTINCT
    CONCAT(UPPER(LEFT(sp.position, 1)), SUBSTRING(sp.position, 2)),
    CASE
        WHEN sp.position LIKE '%Goalkeeper%' THEN 'Goalkeeper'
        WHEN sp.position LIKE '%Back%' OR sp.position LIKE '%Defender%' THEN 'Defender'
        WHEN sp.position LIKE '%midfield%' OR sp.position LIKE '%Midfield%' OR sp.position LIKE '%Winger%' THEN 'Midfielder'
        WHEN sp.position LIKE '%Forward%' OR sp.position LIKE '%Striker%' OR sp.position LIKE '%Attack%' THEN 'Forward'
        ELSE 'Other'
    END AS position_category
FROM staging_player sp
WHERE sp.position IS NOT NULL
  AND TRIM(sp.position) <> '';
  
-- check to see if its correctly inserted 
SELECT *
FROM `Position`
ORDER BY position_name;


-- Country next
-- Need to generate country abreviation for country name 
-- clean up 
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE Country;
SET FOREIGN_KEY_CHECKS = 1;


INSERT INTO Country (country_abbr, country_name) VALUES
    ('AFG', 'Afghanistan'),
    ('ALB', 'Albania'),
    ('ALG', 'Algeria'),
    ('ANG', 'Angola'),
    ('ARG', 'Argentina'),
    ('ARM', 'Armenia'),
    ('ARU', 'Aruba'),
    ('AUS', 'Australia'),
    ('AUT', 'Austria'),
    ('BAR', 'Barbados'),
    ('BEL', 'Belgium'),
    ('BEN', 'Benin'),
    ('BOS', 'Bosnia-Herzegovina'),
    ('BRA', 'Brazil'),
    ('BUL', 'Bulgaria'),
    ('BFA', 'Burkina Faso'),
    ('BDI', 'Burundi'),
    ('CAM', 'Cameroon'),
    ('CAN', 'Canada'),
    ('CAP', 'Cape Verde'),
    ('CEN', 'Central African Republic'),
    ('CHI', 'Chile'),
    ('COL', 'Colombia'),
    ('COM', 'Comoros'),
    ('CON', 'Congo'),
    ('COT', 'Cote d''Ivoire'),
    ('CRO', 'Croatia'),
    ('CUR', 'Curacao'),
    ('CYP', 'Cyprus'),
    ('DRC', 'DR Congo'),
    ('DEN', 'Denmark'),
    ('DOM', 'Dominican Republic'),
    ('ECU', 'Ecuador'),
    ('EGY', 'Egypt'),
    ('ENG', 'England'),
    ('EQU', 'Equatorial Guinea'),
    ('ERI', 'Eritrea'),
    ('FIN', 'Finland'),
    ('FRA', 'France'),
    ('FRE', 'French Guiana'),
    ('GAB', 'Gabon'),
    ('GEO', 'Georgia'),
    ('GER', 'Germany'),
    ('GHA', 'Ghana'),
    ('GRC', 'Greece'),
    ('GRD', 'Grenada'),
    ('GUA', 'Guadeloupe'),
    ('GIN', 'Guinea'),
    ('GNB', 'Guinea-Bissau'),
    ('GUY', 'Guyana'),
    ('HAI', 'Haiti'),
    ('HON', 'Honduras'),
    ('HUN', 'Hungary'),
    ('ICE', 'Iceland'),
    ('IRN', 'Iran'),
    ('IRQ', 'Iraq'),
    ('IRE', 'Ireland'),
    ('ISL', 'Isle of Man'),
    ('ISR', 'Israel'),
    ('ITA', 'Italy'),
    ('JAM', 'Jamaica'),
    ('JAP', 'Japan'),
    ('KEN', 'Kenya'),
    ('KOS', 'Kosovo'),
    ('LIB', 'Liberia'),
    ('LUX', 'Luxembourg'),
    ('MAD', 'Madagascar'),
    ('MLI', 'Mali'),
    ('MLT', 'Malta'),
    ('MAR', 'Martinique'),
    ('MEX', 'Mexico'),
    ('MNE', 'Montenegro'),
    ('MSR', 'Montserrat'),
    ('MOR', 'Morocco'),
    ('MOZ', 'Mozambique'),
    ('NET', 'Netherlands'),
    ('NIG', 'Nigeria'),
    ('MKD', 'North Macedonia'),
    ('NIR', 'Northern Ireland'),
    ('NOR', 'Norway'),
    ('PAR', 'Paraguay'),
    ('PER', 'Peru'),
    ('PHI', 'Philippines'),
    ('POL', 'Poland'),
    ('POR', 'Portugal'),
    ('ROM', 'Romania'),
    ('RUS', 'Russia'),
    ('REU', 'Reunion'),
    ('SAO', 'Sao Tome and Principe'),
    ('SCO', 'Scotland'),
    ('SEN', 'Senegal'),
    ('SER', 'Serbia'),
    ('SIE', 'Sierra Leone'),
    ('SVK', 'Slovakia'),
    ('SVN', 'Slovenia'),
    ('SOM', 'Somalia'),
    ('SOU', 'Southern Sudan'),
    ('SPA', 'Spain'),
    ('SKN', 'St. Kitts & Nevis'),
    ('LCA', 'St. Lucia'),
    ('SUR', 'Suriname'),
    ('SWE', 'Sweden'),
    ('SWI', 'Switzerland'),
    ('SYR', 'Syria'),
    ('TAN', 'Tanzania'),
    ('GAM', 'The Gambia'),
    ('TOG', 'Togo'),
    ('TRI', 'Trinidad and Tobago'),
    ('TUN', 'Tunisia'),
    ('TUR', 'Turkey'),
    ('UKR', 'Ukraine'),
    ('USA', 'United States'),
    ('URU', 'Uruguay'),
    ('UZB', 'Uzbekistan'),
    ('VEN', 'Venezuela'),
    ('WAL', 'Wales'),
    ('ZAM', 'Zambia'),
    ('ZIM', 'Zimbabwe');
-- Fallback in case something is empty 
INSERT INTO Country (country_abbr, country_name)
VALUES ('UNK', 'Unknown');

SELECT COUNT(*) FROM Country;

-- League next
-- League naming problem, data isnt consistent specifically season
-- First need to clean up season inconsistent by throwing away garbage and ONLY keeping YY/YY format
-- Garbage data will stay in staging

-- cleanup to fix error 
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE League;
SET FOREIGN_KEY_CHECKS = 1;


INSERT INTO League (league_name, season_name)
SELECT DISTINCT 
    TRIM(sps.league), 
    TRIM(sps.season)
FROM staging_player_stat sps
WHERE sps.league IS NOT NULL
  AND TRIM(sps.league) <> ''
  AND TRIM(sps.season) REGEXP '^[0-9]{2}/[0-9]{2}$'; -- cleanup

-- check data
SELECT COUNT(*) FROM League; -- 4001 usable data

SELECT *
FROM League
ORDER BY league_name, season_name;

SELECT *
FROM League

