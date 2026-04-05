import readline from "readline/promises";
import { stdin as input, stdout as output } from "process";
import { getDb, closeDb } from "./db.js";

const rl = readline.createInterface({ input, output });

async function prompt(question) {
  const answer = await rl.question(question);
  return answer.trim();
}

async function runSafely(fn) {
  try {
    await fn();
  } catch (err) {
    console.error("Operation failed:", err?.message || err);
  }
}

async function chooseRole() {
  const role = await prompt("Role (admin/analyst): ");
  const normalized = role.toLowerCase();
  if (normalized !== "admin" && normalized !== "analyst") {
    console.log("Unknown role. Defaulting to analyst.");
    return "analyst";
  }
  return normalized;
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

async function ensurePosition(db, positionName, category = null) {
  const [rows] = await db.execute(
    "SELECT position_id FROM `Position` WHERE position_name = ? LIMIT 1",
    [positionName]
  );
  if (rows[0]?.position_id) return rows[0].position_id;
  await db.execute(
    "INSERT INTO `Position` (position_name, position_category) VALUES (?, ?)",
    [positionName, category]
  );
  const [created] = await db.execute(
    "SELECT position_id FROM `Position` WHERE position_name = ? ORDER BY position_id DESC LIMIT 1",
    [positionName]
  );
  return created[0]?.position_id || null;
}

async function ensureClub(db, clubName, countryAbbr, leagueId) {
  const [rows] = await db.execute(
    "SELECT club_id FROM Club WHERE club_name = ? ORDER BY club_id DESC LIMIT 1",
    [clubName]
  );
  if (rows[0]?.club_id) return rows[0].club_id;
  await db.execute(
    "INSERT INTO Club (club_name, country_abbr, league_id) VALUES (?, ?, ?)",
    [clubName, countryAbbr, leagueId]
  );
  const [created] = await db.execute(
    "SELECT club_id FROM Club WHERE club_name = ? ORDER BY club_id DESC LIMIT 1",
    [clubName]
  );
  return created[0]?.club_id || null;
}

async function ensureStadium(db, stadiumName, capacity = null, city = null, country = null) {
  const [rows] = await db.execute(
    "SELECT stadium_id FROM Stadium WHERE stadium_name = ? ORDER BY stadium_id DESC LIMIT 1",
    [stadiumName]
  );
  if (rows[0]?.stadium_id) return rows[0].stadium_id;
  await db.execute(
    "INSERT INTO Stadium (stadium_name, capacity, city, country) VALUES (?, ?, ?, ?)",
    [stadiumName, capacity, city, country]
  );
  const [created] = await db.execute(
    "SELECT stadium_id FROM Stadium WHERE stadium_name = ? ORDER BY stadium_id DESC LIMIT 1",
    [stadiumName]
  );
  return created[0]?.stadium_id || null;
}

async function findPlayerByLastName(db, lastName) {
  const [rows] = await db.execute(
    "SELECT player_id, first_name, last_name, dob FROM Player WHERE LOWER(last_name) = LOWER(?)",
    [lastName]
  );
  return rows;
}

async function selectPlayer(db) {
  const lastName = await prompt("Player last name: ");
  const matches = await findPlayerByLastName(db, lastName);
  if (matches.length === 0) {
    console.log("No players found.");
    return null;
  }
  if (matches.length === 1) return matches[0];

  console.log("Multiple matches:");
  matches.forEach((p, i) => {
    const dob = p.dob ? new Date(p.dob).toISOString().slice(0, 10) : "";
    console.log(`${i + 1}. ${p.first_name} ${p.last_name} (${dob})`);
  });
  const idx = parseInt(await prompt("Choose number: "), 10) - 1;
  return matches[idx] || null;
}

async function findClubByName(db, clubName) {
  const [rows] = await db.execute(
    "SELECT club_id, club_name FROM Club WHERE LOWER(club_name) = LOWER(?)",
    [clubName]
  );
  return rows;
}

async function selectClub(db) {
  const clubName = await prompt("Club name: ");
  const matches = await findClubByName(db, clubName);
  if (matches.length === 0) {
    console.log("No clubs found.");
    return null;
  }
  if (matches.length === 1) return matches[0];

  console.log("Multiple matches:");
  matches.forEach((c, i) => {
    console.log(`${i + 1}. ${c.club_name} (id: ${c.club_id})`);
  });
  const idx = parseInt(await prompt("Choose number: "), 10) - 1;
  return matches[idx] || null;
}

async function createPlayer(db) {
  const first_name = await prompt("First name: ");
  const last_name = await prompt("Last name: ");
  const dob = await prompt("DOB (YYYY-MM-DD): ");
  const positionName = await prompt("Position name: ");
  const positionCategory = await prompt("Position category (optional): ");
  const country_abbr = await prompt("Country abbr (e.g., ENG): ");
  const country_name = await prompt("Country name: ");
  const clubName = await prompt("Club name: ");
  const leagueName = await prompt("League name: ");
  const seasonName = await prompt("Season name (e.g., 2023/24): ");

  await ensureCountry(db, country_abbr, country_name);
  const leagueId = await ensureLeague(db, leagueName, seasonName, country_abbr);
  const clubId = await ensureClub(db, clubName, country_abbr, leagueId);
  const positionId = await ensurePosition(db, positionName, positionCategory || null);

  await db.execute(
    `INSERT INTO Player
      (first_name, last_name, dob, position_id, country_abbr, club_id)
     VALUES (?, ?, ?, ?, ?, ?)`,
    [first_name, last_name, dob || null, positionId, country_abbr, clubId]
  );
  console.log("Player created.");
}

async function readPlayer(db) {
  const player = await selectPlayer(db);
  if (!player) return;
  const [rows] = await db.execute(
    `SELECT p.player_id, p.first_name, p.last_name, p.dob,
            pos.position_name, c.club_name, co.country_name
     FROM Player p
     LEFT JOIN \`Position\` pos ON p.position_id = pos.position_id
     LEFT JOIN Club c ON p.club_id = c.club_id
     LEFT JOIN Country co ON p.country_abbr = co.country_abbr
     WHERE p.player_id = ?`,
    [player.player_id]
  );
  const info = rows[0];
  console.log({
    id: info.player_id,
    name: `${info.first_name} ${info.last_name}`,
    dob: info.dob,
    position: info.position_name || null,
    club: info.club_name || null,
    country: info.country_name || null
  });
}

async function updatePlayer(db) {
  const player = await selectPlayer(db);
  if (!player) return;
  const clubName = await prompt("New club name (leave blank to keep): ");
  const leagueName = await prompt("New league name (leave blank to keep): ");
  const seasonName = await prompt("New season name (leave blank to keep): ");
  const country_abbr = await prompt("New country abbr (leave blank to keep): ");
  const country_name = await prompt("New country name (leave blank to keep): ");
  const positionName = await prompt("New position name (leave blank to keep): ");
  const positionCategory = await prompt("New position category (optional): ");
  const preferredFoot = await prompt("Preferred foot (Left/Right/Both, blank to keep): ");

  const updates = [];
  const params = [];

  if (preferredFoot) {
    updates.push("preferred_foot = ?");
    params.push(preferredFoot);
  }

  let clubId = null;
  if (clubName || leagueName || seasonName || country_abbr) {
    if (country_abbr && country_name) {
      await ensureCountry(db, country_abbr, country_name);
    }
    if (clubName && leagueName && seasonName && country_abbr) {
      const leagueId = await ensureLeague(db, leagueName, seasonName, country_abbr);
      clubId = await ensureClub(db, clubName, country_abbr, leagueId);
    } else if (clubName) {
      const [clubRows] = await db.execute(
        "SELECT club_id FROM Club WHERE club_name = ? ORDER BY club_id DESC LIMIT 1",
        [clubName]
      );
      clubId = clubRows[0]?.club_id || null;
    }
  }

  if (clubId) {
    updates.push("club_id = ?");
    params.push(clubId);
  }

  if (positionName) {
    const positionId = await ensurePosition(db, positionName, positionCategory || null);
    updates.push("position_id = ?");
    params.push(positionId);
  }

  if (updates.length === 0) {
    console.log("No changes provided.");
    return;
  }

  params.push(player.player_id);
  await db.execute(`UPDATE Player SET ${updates.join(", ")} WHERE player_id = ?`, params);
  console.log("Player updated.");
}

async function deletePlayer(db) {
  const player = await selectPlayer(db);
  if (!player) return;
  await db.execute("DELETE FROM Player WHERE player_id = ?", [player.player_id]);
  console.log("Player deleted.");
}

async function createClub(db) {
  const clubName = await prompt("Club name: ");
  const country_abbr = await prompt("Country abbr (e.g., ENG): ");
  const country_name = await prompt("Country name: ");
  const leagueName = await prompt("League name: ");
  const seasonName = await prompt("Season name (e.g., 2023/24): ");
  const stadiumName = await prompt("Stadium name (optional): ");
  const capacityRaw = await prompt("Stadium capacity (optional): ");
  const city = await prompt("Stadium city (optional): ");
  const stadiumCountry = await prompt("Stadium country (optional): ");

  await ensureCountry(db, country_abbr, country_name);
  const league_id = await ensureLeague(db, leagueName, seasonName, country_abbr);

  let stadium_id = null;
  if (stadiumName) {
    const capacity = capacityRaw ? parseInt(capacityRaw, 10) : null;
    stadium_id = await ensureStadium(db, stadiumName, capacity, city || null, stadiumCountry || null);
  }

  await db.execute(
    "INSERT INTO Club (club_name, country_abbr, league_id, stadium_id) VALUES (?, ?, ?, ?)",
    [clubName, country_abbr, league_id, stadium_id]
  );
  console.log("Club created.");
}

async function readClub(db) {
  const club = await selectClub(db);
  if (!club) return;

  const [rows] = await db.execute(
    `SELECT c.club_id, c.club_name, co.country_name, l.league_name, l.season_name, s.stadium_name
     FROM Club c
     JOIN Country co ON c.country_abbr = co.country_abbr
     JOIN League l ON c.league_id = l.league_id
     LEFT JOIN Stadium s ON c.stadium_id = s.stadium_id
     WHERE c.club_id = ?`,
    [club.club_id]
  );
  console.table(rows);
}

async function updateClub(db) {
  const club = await selectClub(db);
  if (!club) return;

  const newName = await prompt("New club name (blank to keep): ");
  const newLeagueName = await prompt("New league name (blank to keep): ");
  const newSeasonName = await prompt("New season name (blank to keep): ");
  const stadiumInput = await prompt("New stadium name (blank to keep, 'none' to clear): ");
  const capacityRaw = await prompt("New stadium capacity (optional): ");
  const city = await prompt("New stadium city (optional): ");
  const stadiumCountry = await prompt("New stadium country (optional): ");

  let league_id = null;
  if (newLeagueName && newSeasonName) {
    const [clubRow] = await db.execute(
      "SELECT country_abbr FROM Club WHERE club_id = ?",
      [club.club_id]
    );
    const country_abbr = clubRow[0]?.country_abbr;
    if (country_abbr) {
      league_id = await ensureLeague(db, newLeagueName, newSeasonName, country_abbr);
    }
  }

  let stadium_id = undefined;
  if (stadiumInput.toLowerCase() === "none") {
    stadium_id = null;
  } else if (stadiumInput) {
    const capacity = capacityRaw ? parseInt(capacityRaw, 10) : null;
    stadium_id = await ensureStadium(db, stadiumInput, capacity, city || null, stadiumCountry || null);
  }

  const fields = [];
  const params = [];
  if (newName) {
    fields.push("club_name = ?");
    params.push(newName);
  }
  if (league_id !== null) {
    fields.push("league_id = ?");
    params.push(league_id);
  }
  if (stadium_id !== undefined) {
    fields.push("stadium_id = ?");
    params.push(stadium_id);
  }

  if (fields.length === 0) {
    console.log("No changes provided.");
    return;
  }

  params.push(club.club_id);
  await db.execute(`UPDATE Club SET ${fields.join(", ")} WHERE club_id = ?`, params);
  console.log("Club updated.");
}

async function deleteClub(db) {
  const club = await selectClub(db);
  if (!club) return;
  await db.execute("DELETE FROM Club WHERE club_id = ?", [club.club_id]);
  console.log("Club deleted.");
}

async function createTransfer(db) {
  const player = await selectPlayer(db);
  if (!player) return;
  const oldClubName = await prompt("Old club (blank for none): ");
  const newClubName = await prompt("New club: ");
  const dateRaw = await prompt("Transfer date (YYYY-MM-DD): ");
  const feeValueRaw = await prompt("Fee value (number, blank for null): ");

  let oldClubId = null;
  if (oldClubName) {
    const [oldRows] = await db.execute(
      "SELECT club_id FROM Club WHERE club_name = ? ORDER BY club_id DESC LIMIT 1",
      [oldClubName]
    );
    oldClubId = oldRows[0]?.club_id || null;
  }
  const [newRows] = await db.execute(
    "SELECT club_id FROM Club WHERE club_name = ? ORDER BY club_id DESC LIMIT 1",
    [newClubName]
  );
  const newClubId = newRows[0]?.club_id || null;
  if (!newClubId) {
    console.log("New club not found. Create the club first.");
    return;
  }

  await db.execute(
    "INSERT INTO Transfer (player_id, old_club_id, new_club_id, transfer_date, transfer_fee) VALUES (?, ?, ?, ?, ?)",
    [
      player.player_id,
      oldClubId,
      newClubId,
      dateRaw || null,
      feeValueRaw ? parseFloat(feeValueRaw) : null
    ]
  );
  console.log("Transfer created.");
}

async function updateTransfer(db) {
  const player = await selectPlayer(db);
  if (!player) return;
  const dateRaw = await prompt("Transfer date to update (YYYY-MM-DD): ");
  const feeValueRaw = await prompt("New fee value (number): ");

  await db.execute(
    "UPDATE Transfer SET transfer_fee = ? WHERE player_id = ? AND transfer_date = ?",
    [feeValueRaw ? parseFloat(feeValueRaw) : null, player.player_id, dateRaw]
  );
  console.log("Transfer updated.");
}

async function deleteTransfer(db) {
  const player = await selectPlayer(db);
  if (!player) return;
  const dateRaw = await prompt("Transfer date to delete (YYYY-MM-DD): ");
  await db.execute(
    "DELETE FROM Transfer WHERE player_id = ? AND transfer_date = ?",
    [player.player_id, dateRaw]
  );
  console.log("Transfer deleted.");
}

async function createMarketValue(db) {
  const player = await selectPlayer(db);
  if (!player) return;
  const dateRaw = await prompt("Market value date (YYYY-MM-DD): ");
  const valueRaw = await prompt("Market value number: ");

  await db.execute(
    "INSERT INTO MarketValue (player_id, market_value_date, market_value) VALUES (?, ?, ?)",
    [player.player_id, dateRaw || null, parseFloat(valueRaw)]
  );
  console.log("Market value created.");
}

async function readMarketValues(db) {
  const player = await selectPlayer(db);
  if (!player) return;
  const [rows] = await db.execute(
    "SELECT market_value_date, market_value FROM MarketValue WHERE player_id = ? ORDER BY market_value_date DESC LIMIT 10",
    [player.player_id]
  );
  console.table(rows.map(v => ({ date: v.market_value_date, value: v.market_value })));
}

async function analystTopMarketValues(db) {
  const [rows] = await db.execute(
    `SELECT p.first_name, p.last_name, mv.market_value, mv.market_value_date
     FROM MarketValue mv
     JOIN (
       SELECT player_id, MAX(market_value_date) AS latest_date
       FROM MarketValue
       GROUP BY player_id
     ) latest ON mv.player_id = latest.player_id AND mv.market_value_date = latest.latest_date
     JOIN Player p ON mv.player_id = p.player_id
     ORDER BY mv.market_value DESC
     LIMIT 10`
  );

  console.table(rows.map(r => ({
    player: `${r.first_name} ${r.last_name}`,
    value: r.market_value,
    date: r.market_value_date
  })));
}

async function analystTransferSpending(db) {
  const [rows] = await db.execute(
    `SELECT c.club_name, SUM(t.transfer_fee) AS total_spent, COUNT(*) AS deals
     FROM Transfer t
     JOIN Club c ON t.new_club_id = c.club_id
     WHERE t.transfer_fee IS NOT NULL
     GROUP BY t.new_club_id
     ORDER BY total_spent DESC
     LIMIT 10`
  );
  console.table(rows.map(r => ({
    club: r.club_name,
    total_spent: r.total_spent,
    deals: r.deals
  })));
}

async function adminMenu(db) {
  while (true) {
    console.log("\nAdmin Menu");
    console.log("1. Create player");
    console.log("2. Read player");
    console.log("3. Update player");
    console.log("4. Delete player");
    console.log("5. Create transfer");
    console.log("6. Update transfer");
    console.log("7. Delete transfer");
    console.log("8. Create market value");
    console.log("9. Read market values");
    console.log("10. Create club");
    console.log("11. Read club");
    console.log("12. Update club");
    console.log("13. Delete club");
    console.log("0. Logout");

    const choice = await prompt("Choose: ");
    if (choice === "0") break;
    if (choice === "1") await runSafely(() => createPlayer(db));
    else if (choice === "2") await runSafely(() => readPlayer(db));
    else if (choice === "3") await runSafely(() => updatePlayer(db));
    else if (choice === "4") await runSafely(() => deletePlayer(db));
    else if (choice === "5") await runSafely(() => createTransfer(db));
    else if (choice === "6") await runSafely(() => updateTransfer(db));
    else if (choice === "7") await runSafely(() => deleteTransfer(db));
    else if (choice === "8") await runSafely(() => createMarketValue(db));
    else if (choice === "9") await runSafely(() => readMarketValues(db));
    else if (choice === "10") await runSafely(() => createClub(db));
    else if (choice === "11") await runSafely(() => readClub(db));
    else if (choice === "12") await runSafely(() => updateClub(db));
    else if (choice === "13") await runSafely(() => deleteClub(db));
  }
}

async function analystMenu(db) {
  while (true) {
    console.log("\nAnalyst Menu");
    console.log("1. Read player");
    console.log("2. Top 10 market values (latest)");
    console.log("3. Transfer spending by club");
    console.log("0. Logout");

    const choice = await prompt("Choose: ");
    if (choice === "0") break;
    if (choice === "1") await runSafely(() => readPlayer(db));
    else if (choice === "2") await runSafely(() => analystTopMarketValues(db));
    else if (choice === "3") await runSafely(() => analystTransferSpending(db));
  }
}

async function main() {
  const db = await getDb();

  console.log("\nSoccer Analytics CLI (MySQL)");
  const role = await chooseRole();

  if (role === "admin") {
    await adminMenu(db);
  } else {
    await analystMenu(db);
  }

  await closeDb();
  rl.close();
}

main().catch(err => {
  console.error(err);
  rl.close();
  process.exit(1);
});
