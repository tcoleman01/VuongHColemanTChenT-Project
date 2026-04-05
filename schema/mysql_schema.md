# MySQL Schema Overview (CS 5200 Project)

## Core Entities (Tables)
- `Country`
- `League`
- `Stadium`
- `Club`
- `Coach`
- `Position`
- `Player`
- `MarketValue`
- `Match`
- `PlayerStats`
- `Transfer`
- `User`
- `Role`
- `UserRole`
- `ScoutReport`

## Key Relationships (Foreign Keys)
- `League.country_abbr` -> `Country.country_abbr`
- `Club.country_abbr` -> `Country.country_abbr`
- `Club.league_id` -> `League.league_id`
- `Club.stadium_id` -> `Stadium.stadium_id`
- `Club.coach_id` -> `Coach.coach_id`
- `Coach.nationality` -> `Country.country_abbr`
- `Coach.club_id` -> `Club.club_id`
- `Player.position_id` -> `Position.position_id`
- `Player.country_abbr` -> `Country.country_abbr`
- `Player.club_id` -> `Club.club_id`
- `MarketValue.player_id` -> `Player.player_id`
- `Match.home_team_id` / `Match.away_team_id` -> `Club.club_id`
- `PlayerStats.player_id` -> `Player.player_id`
- `PlayerStats.match_id` -> `Match.match_id`
- `Transfer.player_id` -> `Player.player_id`
- `Transfer.old_club_id` / `Transfer.new_club_id` -> `Club.club_id`
- `User.country_abbr` -> `Country.country_abbr`
- `UserRole.user_id` -> `User.user_id`
- `UserRole.role_id` -> `Role.role_id`
- `ScoutReport.player_id` -> `Player.player_id`
- `ScoutReport.scout_user_id` -> `User.user_id`

## Primary Keys / Unique Constraints (Highlights)
- `Country`: PK `country_abbr`, unique `country_name`
- `League`: PK `league_id`, unique (`league_name`, `season_name`)
- `Club`: PK `club_id`
- `Coach`: PK `coach_id`
- `Position`: PK `position_id`
- `Player`: PK `player_id`
- `MarketValue`: PK (`player_id`, `market_value_date`)
- `Match`: PK `match_id`, unique (`home_team_id`, `away_team_id`, `match_date`)
- `PlayerStats`: PK (`player_id`, `match_id`)
- `Transfer`: PK `transfer_id`
- `User`: PK `user_id`, unique `username`
- `Role`: PK `role_id`, unique `role_name`
- `UserRole`: PK (`user_id`, `role_id`)
- `ScoutReport`: PK `report_id`

## Notes
- Full DDL lives in `soccer_analytics_db.sql`.
- `Club` and `Coach` reference each other via FKs created with `ALTER TABLE` to avoid circular dependency at creation time.
