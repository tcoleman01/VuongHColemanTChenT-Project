import fs from "fs";
import path from "path";
import { parse } from "csv-parse/sync";
import bcrypt from "bcryptjs";
import { getDb, closeDb } from "../src/db.js";

function resolveDataDir() {
  if (process.env.DATA_DIR && fs.existsSync(process.env.DATA_DIR)) {
    return process.env.DATA_DIR;
  }
  const local = path.resolve(process.cwd(), "data");
  return local;
}

function readCsv(filePath) {
  const content = fs.readFileSync(filePath, "utf-8");
  return parse(content, { columns: true, skip_empty_lines: true });
}

const monthMap = {
  Jan: 0, Feb: 1, Mar: 2, Apr: 3, May: 4, Jun: 5,
  Jul: 6, Aug: 7, Sep: 8, Oct: 9, Nov: 10, Dec: 11
};

function parseDdmmyy(str) {
  if (!str) return null;
  const parts = str.split("-");
  if (parts.length !== 3) return null;
  const day = parseInt(parts[0], 10);
  const mon = monthMap[parts[1]];
  let year = parseInt(parts[2], 10);
  if (Number.isNaN(day) || mon === undefined || Number.isNaN(year)) return null;
  year += year < 50 ? 2000 : 1900;
  const d = new Date(Date.UTC(year, mon, day));
  return Number.isNaN(d.getTime()) ? null : d;
}

function parseLongDate(str) {
  if (!str) return null;
  const d = new Date(str);
  return Number.isNaN(d.getTime()) ? null : d;
}

function parseHeightCm(heightStr) {
  if (!heightStr) return null;
  const normalized = String(heightStr).replace(",", ".");
  const meters = parseFloat(normalized);
  if (Number.isNaN(meters)) return null;
  return Math.round(meters * 100);
}

function parseIntSafe(val) {
  if (val === null || val === undefined || val === "") return null;
  const num = parseInt(val, 10);
  return Number.isNaN(num) ? null : num;
}

function parseFloatSafe(val) {
  if (val === null || val === undefined || val === "") return null;
  const num = parseFloat(val);
  return Number.isNaN(num) ? null : num;
}

async function seedRolesAndUsers(db) {
  const rolesCol = db.collection("roles");
  const usersCol = db.collection("users");

  const roles = [
    { name: "admin", permissions: ["players:crud", "transfers:crud", "market:crud", "stats:crud", "reports:read"] },
    { name: "scout", permissions: ["players:read", "reports:crud"] },
    { name: "analyst", permissions: ["players:read", "stats:read", "market:read", "transfers:read", "reports:read"] }
  ];

  for (const role of roles) {
    await rolesCol.updateOne({ name: role.name }, { $set: role }, { upsert: true });
  }

  const roleDocs = await rolesCol.find().toArray();
  const roleMap = new Map(roleDocs.map(r => [r.name, r._id]));

  const users = [
    { username: "admin", password: "admin123", roles: ["admin"] },
    { username: "scout", password: "scout123", roles: ["scout"] },
    { username: "analyst", password: "analyst123", roles: ["analyst"] }
  ];

  for (const user of users) {
    const password_hash = bcrypt.hashSync(user.password, 10);
    const role_ids = user.roles.map(r => roleMap.get(r)).filter(Boolean);
    await usersCol.updateOne(
      { username: user.username },
      { $set: { username: user.username, password_hash, role_ids } },
      { upsert: true }
    );
  }
}

async function seedData() {
  const db = await getDb();
  await seedRolesAndUsers(db);

  const dataDir = resolveDataDir();
  const playerPath = path.join(dataDir, "player.csv");
  const statsPath = path.join(dataDir, "player_stat.csv");
  const marketPath = path.join(dataDir, "market_value.csv");
  const transferPath = path.join(dataDir, "transfer_history.csv");

  if (![playerPath, statsPath, marketPath, transferPath].every(fs.existsSync)) {
    throw new Error(`CSV files not found in ${dataDir}`);
  }

  const playersCsv = readCsv(playerPath);
  const statsCsv = readCsv(statsPath);
  const marketCsv = readCsv(marketPath);
  const transferCsv = readCsv(transferPath);

  const clubsCol = db.collection("clubs");
  const leaguesCol = db.collection("leagues");
  const seasonsCol = db.collection("seasons");
  const countriesCol = db.collection("countries");
  const playersCol = db.collection("players");
  const statsCol = db.collection("player_stats");
  const marketCol = db.collection("market_values");
  const transferCol = db.collection("transfers");

  const clubNames = new Set();
  for (const row of playersCsv) if (row.club) clubNames.add(row.club);
  for (const row of marketCsv) if (row.mv_club) clubNames.add(row.mv_club);
  for (const row of transferCsv) {
    if (row.old_club) clubNames.add(row.old_club);
    if (row.new_club) clubNames.add(row.new_club);
  }

  for (const name of clubNames) {
    await clubsCol.updateOne({ name }, { $set: { name } }, { upsert: true });
  }
  const clubs = await clubsCol.find().toArray();
  const clubMap = new Map(clubs.map(c => [c.name, c._id]));

  const countryNames = new Set();
  for (const row of playersCsv) {
    if (row.citizenship1) countryNames.add(row.citizenship1);
    if (row.citizenship2) countryNames.add(row.citizenship2);
  }
  for (const name of countryNames) {
    await countriesCol.updateOne({ name }, { $set: { name } }, { upsert: true });
  }

  const leagueNames = new Set(statsCsv.map(r => r.league).filter(Boolean));
  for (const name of leagueNames) {
    await leaguesCol.updateOne({ name }, { $set: { name } }, { upsert: true });
  }
  const leagues = await leaguesCol.find().toArray();
  const leagueMap = new Map(leagues.map(l => [l.name, l._id]));

  const seasonCodes = new Set(statsCsv.map(r => r.season).filter(Boolean));
  for (const code of seasonCodes) {
    await seasonsCol.updateOne({ code }, { $set: { code } }, { upsert: true });
  }
  const seasons = await seasonsCol.find().toArray();
  const seasonMap = new Map(seasons.map(s => [s.code, s._id]));

  for (const row of playersCsv) {
    const citizenships = [row.citizenship1, row.citizenship2].filter(Boolean);
    const doc = {
      first_name: row.first_name,
      last_name: row.last_name,
      dob: parseDdmmyy(row.dob),
      name_in_home_country: row.name_in_home_country || null,
      place_of_birth: row.place_of_birth || null,
      height_cm: parseHeightCm(row.height),
      position: row.position,
      foot: row.foot || null,
      club_id: clubMap.get(row.club),
      join_date: parseDdmmyy(row.join_date),
      contract_expires: parseDdmmyy(row["contract_expires:"]),
      profile: { citizenships }
    };

    await playersCol.updateOne(
      { first_name: doc.first_name, last_name: doc.last_name, dob: doc.dob },
      { $set: doc },
      { upsert: true }
    );
  }

  const players = await playersCol.find().toArray();
  const playerMap = new Map();
  for (const p of players) {
    const key = `${p.first_name}|${p.last_name}`;
    if (!playerMap.has(key)) playerMap.set(key, p._id);
  }

  for (const row of statsCsv) {
    const key = `${row.first_name}|${row.last_name}`;
    const player_id = playerMap.get(key);
    if (!player_id) continue;
    const season_id = seasonMap.get(row.season);
    const league_id = leagueMap.get(row.league);
    if (!season_id || !league_id) continue;

    await statsCol.updateOne(
      { player_id, season_id, league_id },
      {
        $set: {
          player_id,
          season_id,
          league_id,
          appear: parseIntSafe(row.appear),
          goal: parseIntSafe(row.goal),
          assist: parseIntSafe(row.assist),
          play_time: parseIntSafe(row.play_time)
        }
      },
      { upsert: true }
    );
  }

  for (const row of marketCsv) {
    const key = `${row.first_name}|${row.last_name}`;
    const player_id = playerMap.get(key);
    if (!player_id) continue;

    await marketCol.updateOne(
      { player_id, mv_date: parseLongDate(row.mv_date) },
      {
        $set: {
          player_id,
          mv_date: parseLongDate(row.mv_date),
          mv_value: parseFloatSafe(row.mv_value) || 0,
          mv_unit: row.mv_unit || "€",
          mv_club_id: clubMap.get(row.mv_club) || null
        }
      },
      { upsert: true }
    );
  }

  for (const row of transferCsv) {
    const key = `${row.first_name}|${row.last_name}`;
    const player_id = playerMap.get(key);
    if (!player_id) continue;

    await transferCol.updateOne(
      { player_id, date: parseDdmmyy(row.date) },
      {
        $set: {
          player_id,
          date: parseDdmmyy(row.date),
          fee_value: parseFloatSafe(row.fee_value),
          fee_text: row.fee || null,
          old_club_id: clubMap.get(row.old_club) || null,
          new_club_id: clubMap.get(row.new_club) || null
        }
      },
      { upsert: true }
    );
  }

  await closeDb();
  console.log("Seed complete.");
}

seedData().catch(err => {
  console.error(err);
  process.exit(1);
});
