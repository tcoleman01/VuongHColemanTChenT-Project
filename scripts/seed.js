import { getDb, closeDb } from "../src/db.js";

async function getId(db, table, idCol, whereCol, whereVal) {
  const [rows] = await db.execute(
    `SELECT ${idCol} AS id FROM ${table} WHERE ${whereCol} = ? LIMIT 1`,
    [whereVal]
  );
  return rows[0]?.id || null;
}

async function ensureCountry(db, abbr, name) {
  await db.execute(
    "INSERT INTO Country (country_abbr, country_name) VALUES (?, ?) ON DUPLICATE KEY UPDATE country_name = VALUES(country_name)",
    [abbr, name]
  );
  return abbr;
}

async function ensureLeague(db, leagueName, seasonName, countryAbbr) {
  await db.execute(
    "INSERT INTO League (league_name, season_name, country_abbr) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE country_abbr = VALUES(country_abbr)",
    [leagueName, seasonName, countryAbbr]
  );
  const [rows] = await db.execute(
    "SELECT league_id FROM League WHERE league_name = ? AND season_name = ? LIMIT 1",
    [leagueName, seasonName]
  );
  return rows[0]?.league_id || null;
}

async function ensureStadium(db, stadiumName, capacity, city, country) {
  const existingId = await getId(db, "Stadium", "stadium_id", "stadium_name", stadiumName);
  if (existingId) return existingId;
  await db.execute(
    "INSERT INTO Stadium (stadium_name, capacity, city, country) VALUES (?, ?, ?, ?)",
    [stadiumName, capacity, city, country]
  );
  return getId(db, "Stadium", "stadium_id", "stadium_name", stadiumName);
}

async function ensurePosition(db, positionName, category = null) {
  const existingId = await getId(db, "Position", "position_id", "position_name", positionName);
  if (existingId) return existingId;
  await db.execute(
    "INSERT INTO `Position` (position_name, position_category) VALUES (?, ?)",
    [positionName, category]
  );
  return getId(db, "Position", "position_id", "position_name", positionName);
}

async function ensureClub(db, clubName, countryAbbr, leagueId, stadiumId = null) {
  const existingId = await getId(db, "Club", "club_id", "club_name", clubName);
  if (existingId) return existingId;
  await db.execute(
    "INSERT INTO Club (club_name, country_abbr, league_id, stadium_id) VALUES (?, ?, ?, ?)",
    [clubName, countryAbbr, leagueId, stadiumId]
  );
  return getId(db, "Club", "club_id", "club_name", clubName);
}

async function createPlayer(db, player) {
  const [existing] = await db.execute(
    "SELECT player_id FROM Player WHERE first_name = ? AND last_name = ? AND dob = ? LIMIT 1",
    [player.first_name, player.last_name, player.dob]
  );
  if (existing[0]?.player_id) return existing[0].player_id;
  await db.execute(
    `INSERT INTO Player
      (first_name, last_name, dob, place_of_birth, height_cm, preferred_foot, position_id, country_abbr, club_id)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    [
      player.first_name,
      player.last_name,
      player.dob,
      player.place_of_birth,
      player.height_cm,
      player.preferred_foot,
      player.position_id,
      player.country_abbr,
      player.club_id
    ]
  );
  const [rows] = await db.execute(
    "SELECT player_id FROM Player WHERE first_name = ? AND last_name = ? AND dob = ? ORDER BY player_id DESC LIMIT 1",
    [player.first_name, player.last_name, player.dob]
  );
  return rows[0]?.player_id || null;
}

async function seedData() {
  const db = await getDb();

  await ensureCountry(db, "ENG", "England");
  await ensureCountry(db, "ESP", "Spain");

  const premierId = await ensureLeague(db, "Premier League", "2023/24", "ENG");
  const laLigaId = await ensureLeague(db, "La Liga", "2023/24", "ESP");

  const emiratesId = await ensureStadium(db, "Emirates Stadium", 60704, "London", "England");
  const campNouId = await ensureStadium(db, "Camp Nou", 99354, "Barcelona", "Spain");

  const arsenalId = await ensureClub(db, "Arsenal", "ENG", premierId, emiratesId);
  const barcaId = await ensureClub(db, "Barcelona", "ESP", laLigaId, campNouId);

  const forwardId = await ensurePosition(db, "Forward", "Attack");
  const midfieldId = await ensurePosition(db, "Midfielder", "Midfield");

  const sakaId = await createPlayer(db, {
    first_name: "Bukayo",
    last_name: "Saka",
    dob: "2001-09-05",
    place_of_birth: "London",
    height_cm: 178,
    preferred_foot: "Left",
    position_id: forwardId,
    country_abbr: "ENG",
    club_id: arsenalId
  });

  const pedriId = await createPlayer(db, {
    first_name: "Pedri",
    last_name: "Gonzalez",
    dob: "2002-11-25",
    place_of_birth: "Tegueste",
    height_cm: 174,
    preferred_foot: "Right",
    position_id: midfieldId,
    country_abbr: "ESP",
    club_id: barcaId
  });

  await db.execute(
    "INSERT INTO MarketValue (player_id, market_value_date, market_value) VALUES (?, ?, ?)",
    [sakaId, "2024-01-01", 95000000]
  );
  await db.execute(
    "INSERT INTO MarketValue (player_id, market_value_date, market_value) VALUES (?, ?, ?)",
    [pedriId, "2024-01-01", 100000000]
  );

  await db.execute(
    "INSERT INTO Transfer (player_id, old_club_id, new_club_id, transfer_date, transfer_fee) VALUES (?, ?, ?, ?, ?)",
    [pedriId, null, barcaId, "2020-09-01", 5000000]
  );

  await db.execute(
    "INSERT INTO `Match` (home_team_id, away_team_id, match_date, home_score, away_score, home_result, away_result) VALUES (?, ?, ?, ?, ?, ?, ?)",
    [arsenalId, barcaId, "2024-03-15", 2, 1, "Win", "Loss"]
  );

  await closeDb();
  console.log("Seed complete.");
}

seedData().catch(err => {
  console.error(err);
  process.exit(1);
});
