# MySQL Schema Overview (CS 5200 Project)

## Tables
- `Country`
- `League`
- `Stadium`
- `Club`
- `Coach`
- `Position`
- `Player`
- `MarketValue`
- `Match`
- `Transfer`
- `SeasonPerformance`
- `MatchPerformance`
- `User`

## Key Relationships (Foreign Keys)
- `League.country_abbr` -> `Country.country_abbr`
- `Club.country_abbr` -> `Country.country_abbr`
- `Club.stadium_id` -> `Stadium.stadium_id`
- `Club.coach_id` -> `Coach.coach_id`
- `Coach.nationality` -> `Country.country_abbr`
- `Coach.club_id` -> `Club.club_id`
- `Player.position_id` -> `Position.position_id`
- `Player.country_abbr` -> `Country.country_abbr`
- `Player.club_id` -> `Club.club_id`
- `MarketValue.player_id` -> `Player.player_id`
- `Match.home_team_id` / `Match.away_team_id` -> `Club.club_id`
- `Transfer.player_id` -> `Player.player_id`
- `Transfer.old_club_id` / `Transfer.new_club_id` -> `Club.club_id`
- `SeasonPerformance.player_id` -> `Player.player_id`
- `SeasonPerformance.league_id` -> `League.league_id`
- `MatchPerformance.match_id` -> `Match.match_id`
- `MatchPerformance.player_id` -> `Player.player_id`

## Primary Keys / Unique Constraints (Highlights)
- `Country`: PK `country_abbr`, unique `country_name`
- `League`: PK `league_id`, unique (`league_name`, `season_name`)
- `Club`: PK `club_id`
- `Coach`: PK `coach_id`
- `Position`: PK `position_id`
- `Player`: PK `player_id`
- `MarketValue`: PK (`player_id`, `market_value_date`)
- `Match`: PK `match_id`, unique (`home_team_id`, `away_team_id`, `match_date`)
- `Transfer`: PK `transfer_id`
- `SeasonPerformance`: PK (`player_id`, `league_id`)
- `MatchPerformance`: PK (`match_id`, `player_id`)
- `User`: PK `username`

## Notes
- Full DDL lives in `soccer_analytics_db.sql`.
- `Club` and `Coach` reference each other via FKs created with `ALTER TABLE` to avoid circular dependency at creation time.
- Server-side objects included: `fn_player_age`, `sp_record_transfer`, triggers on `Match`, and the nightly `ev_update_match_results` event.
