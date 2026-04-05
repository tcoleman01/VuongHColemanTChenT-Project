-- MySQL dump 10.13  Distrib 9.6.0, for macos26.3 (arm64)
--
-- Host: localhost    Database: soccer_analytics_db
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `soccer_analytics_db`
--

/*!40000 DROP DATABASE IF EXISTS `soccer_analytics_db`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `soccer_analytics_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `soccer_analytics_db`;

--
-- Table structure for table `Club`
--

DROP TABLE IF EXISTS `Club`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Club` (
  `club_id` int NOT NULL AUTO_INCREMENT,
  `club_name` varchar(100) NOT NULL,
  `country_abbr` char(3) NOT NULL,
  `league_id` int NOT NULL,
  `stadium_id` int DEFAULT NULL,
  `coach_id` int DEFAULT NULL,
  PRIMARY KEY (`club_id`),
  KEY `fk_club_country` (`country_abbr`),
  KEY `fk_club_league` (`league_id`),
  KEY `fk_club_stadium` (`stadium_id`),
  KEY `fk_club_coach` (`coach_id`),
  CONSTRAINT `fk_club_coach` FOREIGN KEY (`coach_id`) REFERENCES `Coach` (`coach_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_club_country` FOREIGN KEY (`country_abbr`) REFERENCES `Country` (`country_abbr`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_club_league` FOREIGN KEY (`league_id`) REFERENCES `League` (`league_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_club_stadium` FOREIGN KEY (`stadium_id`) REFERENCES `Stadium` (`stadium_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Club`
--

LOCK TABLES `Club` WRITE;
/*!40000 ALTER TABLE `Club` DISABLE KEYS */;
INSERT INTO `Club` VALUES (1,'Arsenal','ENG',1,1,NULL),(2,'Barcelona','ESP',2,2,NULL);
/*!40000 ALTER TABLE `Club` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Coach`
--

DROP TABLE IF EXISTS `Coach`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Coach` (
  `coach_id` int NOT NULL AUTO_INCREMENT,
  `first_name` varchar(50) NOT NULL,
  `last_name` varchar(50) NOT NULL,
  `dob` date DEFAULT NULL,
  `nationality` char(3) DEFAULT NULL,
  `club_id` int DEFAULT NULL,
  PRIMARY KEY (`coach_id`),
  KEY `fk_coach_country` (`nationality`),
  KEY `fk_coach_club` (`club_id`),
  CONSTRAINT `fk_coach_club` FOREIGN KEY (`club_id`) REFERENCES `Club` (`club_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_coach_country` FOREIGN KEY (`nationality`) REFERENCES `Country` (`country_abbr`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Coach`
--

LOCK TABLES `Coach` WRITE;
/*!40000 ALTER TABLE `Coach` DISABLE KEYS */;
/*!40000 ALTER TABLE `Coach` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Country`
--

DROP TABLE IF EXISTS `Country`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Country` (
  `country_abbr` char(3) NOT NULL,
  `country_name` varchar(100) NOT NULL,
  PRIMARY KEY (`country_abbr`),
  UNIQUE KEY `ak_country_name` (`country_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Country`
--

LOCK TABLES `Country` WRITE;
/*!40000 ALTER TABLE `Country` DISABLE KEYS */;
INSERT INTO `Country` VALUES ('ENG','England'),('ESP','Spain');
/*!40000 ALTER TABLE `Country` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `League`
--

DROP TABLE IF EXISTS `League`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `League` (
  `league_id` int NOT NULL AUTO_INCREMENT,
  `league_name` varchar(100) NOT NULL,
  `season_name` varchar(50) NOT NULL,
  `country_abbr` char(3) NOT NULL,
  PRIMARY KEY (`league_id`),
  UNIQUE KEY `pak_league` (`league_name`,`season_name`),
  KEY `fk_league_country` (`country_abbr`),
  CONSTRAINT `fk_league_country` FOREIGN KEY (`country_abbr`) REFERENCES `Country` (`country_abbr`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `League`
--

LOCK TABLES `League` WRITE;
/*!40000 ALTER TABLE `League` DISABLE KEYS */;
INSERT INTO `League` VALUES (1,'Premier League','2023/24','ENG'),(2,'La Liga','2023/24','ESP');
/*!40000 ALTER TABLE `League` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `MarketValue`
--

DROP TABLE IF EXISTS `MarketValue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `MarketValue` (
  `player_id` int NOT NULL,
  `market_value_date` date NOT NULL,
  `market_value` decimal(15,2) NOT NULL,
  PRIMARY KEY (`player_id`,`market_value_date`),
  CONSTRAINT `fk_mv_player` FOREIGN KEY (`player_id`) REFERENCES `Player` (`player_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `MarketValue`
--

LOCK TABLES `MarketValue` WRITE;
/*!40000 ALTER TABLE `MarketValue` DISABLE KEYS */;
INSERT INTO `MarketValue` VALUES (1,'2024-01-01',95000000.00),(2,'2024-01-01',100000000.00);
/*!40000 ALTER TABLE `MarketValue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Match`
--

DROP TABLE IF EXISTS `Match`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Match` (
  `match_id` int NOT NULL AUTO_INCREMENT,
  `home_team_id` int NOT NULL,
  `away_team_id` int NOT NULL,
  `match_date` date NOT NULL,
  `home_score` int DEFAULT '0',
  `away_score` int DEFAULT '0',
  `home_result` enum('Win','Loss','Draw') DEFAULT NULL,
  `away_result` enum('Win','Loss','Draw') DEFAULT NULL,
  PRIMARY KEY (`match_id`),
  UNIQUE KEY `pak_match` (`home_team_id`,`away_team_id`,`match_date`),
  KEY `fk_match_away` (`away_team_id`),
  CONSTRAINT `fk_match_away` FOREIGN KEY (`away_team_id`) REFERENCES `Club` (`club_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_match_home` FOREIGN KEY (`home_team_id`) REFERENCES `Club` (`club_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Match`
--

LOCK TABLES `Match` WRITE;
/*!40000 ALTER TABLE `Match` DISABLE KEYS */;
INSERT INTO `Match` VALUES (1,1,2,'2024-03-15',2,1,'Win','Loss');
/*!40000 ALTER TABLE `Match` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_unicode_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_match_validate_insert` BEFORE INSERT ON `match` FOR EACH ROW BEGIN
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
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_unicode_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_match_validate_update` BEFORE UPDATE ON `match` FOR EACH ROW BEGIN
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
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `MatchPerformance`
--

DROP TABLE IF EXISTS `MatchPerformance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `MatchPerformance` (
  `match_id` int NOT NULL,
  `player_id` int NOT NULL,
  `play_time` int DEFAULT '0',
  `performance_rating` decimal(4,2) DEFAULT NULL,
  PRIMARY KEY (`match_id`,`player_id`),
  KEY `fk_mp_player` (`player_id`),
  CONSTRAINT `fk_mp_match` FOREIGN KEY (`match_id`) REFERENCES `Match` (`match_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_mp_player` FOREIGN KEY (`player_id`) REFERENCES `Player` (`player_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `MatchPerformance`
--

LOCK TABLES `MatchPerformance` WRITE;
/*!40000 ALTER TABLE `MatchPerformance` DISABLE KEYS */;
/*!40000 ALTER TABLE `MatchPerformance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Player`
--

DROP TABLE IF EXISTS `Player`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Player` (
  `player_id` int NOT NULL AUTO_INCREMENT,
  `first_name` varchar(50) NOT NULL,
  `last_name` varchar(50) NOT NULL,
  `dob` date DEFAULT NULL,
  `place_of_birth` varchar(100) DEFAULT NULL,
  `height_cm` decimal(5,2) DEFAULT NULL,
  `preferred_foot` enum('Left','Right','Both') DEFAULT NULL,
  `position_id` int DEFAULT NULL,
  `country_abbr` char(3) DEFAULT NULL,
  `club_id` int DEFAULT NULL,
  PRIMARY KEY (`player_id`),
  KEY `fk_player_position` (`position_id`),
  KEY `fk_player_country` (`country_abbr`),
  KEY `fk_player_club` (`club_id`),
  CONSTRAINT `fk_player_club` FOREIGN KEY (`club_id`) REFERENCES `Club` (`club_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_player_country` FOREIGN KEY (`country_abbr`) REFERENCES `Country` (`country_abbr`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_player_position` FOREIGN KEY (`position_id`) REFERENCES `Position` (`position_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Player`
--

LOCK TABLES `Player` WRITE;
/*!40000 ALTER TABLE `Player` DISABLE KEYS */;
INSERT INTO `Player` VALUES (1,'Bukayo','Saka','2001-09-05','London',178.00,'Left',1,'ENG',1),(2,'Pedri','Gonzalez','2002-11-25','Tegueste',174.00,'Right',2,'ESP',2);
/*!40000 ALTER TABLE `Player` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Position`
--

DROP TABLE IF EXISTS `Position`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Position` (
  `position_id` int NOT NULL AUTO_INCREMENT,
  `position_name` varchar(50) NOT NULL,
  `position_category` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`position_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Position`
--

LOCK TABLES `Position` WRITE;
/*!40000 ALTER TABLE `Position` DISABLE KEYS */;
INSERT INTO `Position` VALUES (1,'Forward','Attack'),(2,'Midfielder','Midfield');
/*!40000 ALTER TABLE `Position` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `SeasonPerformance`
--

DROP TABLE IF EXISTS `SeasonPerformance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SeasonPerformance` (
  `player_id` int NOT NULL,
  `league_id` int NOT NULL,
  `appearance_count` int DEFAULT '0',
  `goal_count` int DEFAULT '0',
  `assist_count` int DEFAULT '0',
  `play_time` int DEFAULT '0',
  PRIMARY KEY (`player_id`,`league_id`),
  KEY `fk_sp_league` (`league_id`),
  CONSTRAINT `fk_sp_league` FOREIGN KEY (`league_id`) REFERENCES `League` (`league_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_sp_player` FOREIGN KEY (`player_id`) REFERENCES `Player` (`player_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `SeasonPerformance`
--

LOCK TABLES `SeasonPerformance` WRITE;
/*!40000 ALTER TABLE `SeasonPerformance` DISABLE KEYS */;
/*!40000 ALTER TABLE `SeasonPerformance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Stadium`
--

DROP TABLE IF EXISTS `Stadium`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Stadium` (
  `stadium_id` int NOT NULL AUTO_INCREMENT,
  `stadium_name` varchar(100) NOT NULL,
  `capacity` int DEFAULT NULL,
  `street_number` varchar(20) DEFAULT NULL,
  `street_name` varchar(100) DEFAULT NULL,
  `city` varchar(100) DEFAULT NULL,
  `country` varchar(100) DEFAULT NULL,
  `phone_number` varchar(25) DEFAULT NULL,
  PRIMARY KEY (`stadium_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Stadium`
--

LOCK TABLES `Stadium` WRITE;
/*!40000 ALTER TABLE `Stadium` DISABLE KEYS */;
INSERT INTO `Stadium` VALUES (1,'Emirates Stadium',60704,NULL,NULL,'London','England',NULL),(2,'Camp Nou',99354,NULL,NULL,'Barcelona','Spain',NULL);
/*!40000 ALTER TABLE `Stadium` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Transfer`
--

DROP TABLE IF EXISTS `Transfer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Transfer` (
  `transfer_id` int NOT NULL AUTO_INCREMENT,
  `player_id` int NOT NULL,
  `old_club_id` int DEFAULT NULL,
  `new_club_id` int NOT NULL,
  `transfer_date` date NOT NULL,
  `transfer_fee` decimal(15,2) DEFAULT NULL,
  PRIMARY KEY (`transfer_id`),
  KEY `fk_transfer_player` (`player_id`),
  KEY `fk_transfer_oldclub` (`old_club_id`),
  KEY `fk_transfer_newclub` (`new_club_id`),
  CONSTRAINT `fk_transfer_newclub` FOREIGN KEY (`new_club_id`) REFERENCES `Club` (`club_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_transfer_oldclub` FOREIGN KEY (`old_club_id`) REFERENCES `Club` (`club_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_transfer_player` FOREIGN KEY (`player_id`) REFERENCES `Player` (`player_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Transfer`
--

LOCK TABLES `Transfer` WRITE;
/*!40000 ALTER TABLE `Transfer` DISABLE KEYS */;
INSERT INTO `Transfer` VALUES (1,2,NULL,2,'2020-09-01',5000000.00);
/*!40000 ALTER TABLE `Transfer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'soccer_analytics_db'
--
/*!50106 SET @save_time_zone= @@TIME_ZONE */ ;
/*!50106 DROP EVENT IF EXISTS `ev_update_match_results` */;
DELIMITER ;;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;;
/*!50003 SET character_set_client  = utf8mb4 */ ;;
/*!50003 SET character_set_results = utf8mb4 */ ;;
/*!50003 SET collation_connection  = utf8mb4_unicode_ci */ ;;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;;
/*!50003 SET @saved_time_zone      = @@time_zone */ ;;
/*!50003 SET time_zone             = 'SYSTEM' */ ;;
/*!50106 CREATE*/ /*!50117 DEFINER=`root`@`localhost`*/ /*!50106 EVENT `ev_update_match_results` ON SCHEDULE EVERY 1 DAY STARTS '2026-04-04 21:57:55' ON COMPLETION NOT PRESERVE ENABLE DO UPDATE `Match`
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
    WHERE home_result IS NULL OR away_result IS NULL */ ;;
/*!50003 SET time_zone             = @saved_time_zone */ ;;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;;
/*!50003 SET character_set_client  = @saved_cs_client */ ;;
/*!50003 SET character_set_results = @saved_cs_results */ ;;
/*!50003 SET collation_connection  = @saved_col_connection */ ;;
DELIMITER ;
/*!50106 SET TIME_ZONE= @save_time_zone */ ;

--
-- Dumping routines for database 'soccer_analytics_db'
--
--
-- WARNING: can't read the INFORMATION_SCHEMA.libraries table. It's most probably an old server 8.0.45.
--
--
-- WARNING: can't read the INFORMATION_SCHEMA.libraries table. It's most probably an old server 8.0.45.
--
/*!50003 DROP FUNCTION IF EXISTS `fn_player_age` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_unicode_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` FUNCTION `fn_player_age`(p_dob DATE) RETURNS int
    DETERMINISTIC
BEGIN
    IF p_dob IS NULL THEN
        RETURN NULL;
    END IF;
    RETURN TIMESTAMPDIFF(YEAR, p_dob, CURDATE());
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
--
-- WARNING: can't read the INFORMATION_SCHEMA.libraries table. It's most probably an old server 8.0.45.
--
/*!50003 DROP PROCEDURE IF EXISTS `sp_record_transfer` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_unicode_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_record_transfer`(
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
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-04 21:59:43
