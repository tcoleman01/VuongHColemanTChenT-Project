# MongoDB Schema Overview (CS 5200 Project)

## Containers (Collections)
- `players`
- `clubs`
- `leagues`
- `seasons`
- `player_stats`
- `market_values`
- `transfers`
- `countries`
- `users`
- `roles`
- `scout_reports`

## Embedded Objects and Arrays
- `players.profile` is an embedded object with `citizenships` (array of strings).
- `users.role_ids` is an array of `ObjectId` references to `roles`.
- `scout_reports.tags` is an array of strings.
- `scout_reports.ratings` is an embedded object with `technique`, `speed`, `mentality`.

## References (Foreign-Key-Like)
- `players.club_id` -> `clubs._id`
- `player_stats.player_id` -> `players._id`
- `player_stats.season_id` -> `seasons._id`
- `player_stats.league_id` -> `leagues._id`
- `market_values.player_id` -> `players._id`
- `market_values.mv_club_id` -> `clubs._id`
- `transfers.player_id` -> `players._id`
- `transfers.old_club_id` / `transfers.new_club_id` -> `clubs._id`
- `scout_reports.player_id` -> `players._id`
- `scout_reports.scout_user_id` -> `users._id`

## Integrity Constraints
- JSON Schema validators are enforced in `scripts/init-schema.js`.
- Unique indexes:
  - `players`: `first_name + last_name + dob`
  - `clubs`: `name`
  - `leagues`: `name`
  - `seasons`: `code`
  - `countries`: `name`
  - `roles`: `name`
  - `users`: `username`
  - `player_stats`: `player_id + season_id + league_id`

## Notes for UML
- Use simple line + arrow for relationships (no crowfoot notation).
