import readline from "readline/promises";
import { stdin as input, stdout as output } from "process";
import bcrypt from "bcryptjs";
import { ObjectId } from "mongodb";
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

async function login(db) {
  const username = await prompt("Username: ");
  const password = await prompt("Password: ");
  const user = await db.collection("users").findOne({ username });
  if (!user) return null;
  const ok = bcrypt.compareSync(password, user.password_hash);
  if (!ok) return null;

  const roles = await db.collection("roles").find({ _id: { $in: user.role_ids } }).toArray();
  return { user, roles };
}

function hasRole(roles, name) {
  return roles.some(r => r.name === name);
}

async function findPlayerByLastName(db, lastName) {
  return db.collection("players").find({ last_name: new RegExp(`^${lastName}$`, "i") }).toArray();
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
    const dob = p.dob ? p.dob.toISOString().slice(0, 10) : "";
    console.log(`${i + 1}. ${p.first_name} ${p.last_name} (${dob})`);
  });
  const idx = parseInt(await prompt("Choose number: "), 10) - 1;
  return matches[idx] || null;
}

async function createPlayer(db) {
  const first_name = await prompt("First name: ");
  const last_name = await prompt("Last name: ");
  const dobRaw = await prompt("DOB (YYYY-MM-DD): ");
  const position = await prompt("Position: ");
  const clubName = await prompt("Club name: ");

  const dob = dobRaw ? new Date(dobRaw) : null;
  const club = await db.collection("clubs").findOneAndUpdate(
    { name: clubName },
    { $set: { name: clubName } },
    { upsert: true, returnDocument: "after" }
  );

  const doc = {
    first_name,
    last_name,
    dob,
    position,
    club_id: club.value._id,
    join_date: null,
    contract_expires: null,
    profile: { citizenships: [] }
  };

  await db.collection("players").insertOne(doc);
  console.log("Player created.");
}

async function readPlayer(db) {
  const player = await selectPlayer(db);
  if (!player) return;
  const club = await db.collection("clubs").findOne({ _id: player.club_id });
  console.log({
    id: player._id,
    name: `${player.first_name} ${player.last_name}`,
    dob: player.dob,
    position: player.position,
    club: club?.name || null,
    citizenships: player.profile?.citizenships || []
  });
}

async function updatePlayer(db) {
  const player = await selectPlayer(db);
  if (!player) return;
  const clubName = await prompt("New club name (leave blank to keep): ");
  const contractRaw = await prompt("New contract expires (YYYY-MM-DD, blank to keep): ");

  const updates = {};
  if (clubName) {
    const club = await db.collection("clubs").findOneAndUpdate(
      { name: clubName },
      { $set: { name: clubName } },
      { upsert: true, returnDocument: "after" }
    );
    updates.club_id = club.value._id;
  }
  if (contractRaw) updates.contract_expires = new Date(contractRaw);

  if (Object.keys(updates).length === 0) {
    console.log("No changes provided.");
    return;
  }
  await db.collection("players").updateOne({ _id: player._id }, { $set: updates });
  console.log("Player updated.");
}

async function deletePlayer(db) {
  const player = await selectPlayer(db);
  if (!player) return;
  await db.collection("players").deleteOne({ _id: player._id });
  console.log("Player deleted.");
}

async function createTransfer(db) {
  const player = await selectPlayer(db);
  if (!player) return;
  const oldClubName = await prompt("Old club: ");
  const newClubName = await prompt("New club: ");
  const dateRaw = await prompt("Transfer date (YYYY-MM-DD): ");
  const feeValueRaw = await prompt("Fee value (number): ");

  const oldClub = await db.collection("clubs").findOneAndUpdate(
    { name: oldClubName },
    { $set: { name: oldClubName } },
    { upsert: true, returnDocument: "after" }
  );
  const newClub = await db.collection("clubs").findOneAndUpdate(
    { name: newClubName },
    { $set: { name: newClubName } },
    { upsert: true, returnDocument: "after" }
  );

  await db.collection("transfers").insertOne({
    player_id: player._id,
    date: new Date(dateRaw),
    fee_value: feeValueRaw ? parseFloat(feeValueRaw) : null,
    fee_text: feeValueRaw ? null : null,
    old_club_id: oldClub.value._id,
    new_club_id: newClub.value._id
  });

  console.log("Transfer created.");
}

async function updateTransfer(db) {
  const player = await selectPlayer(db);
  if (!player) return;
  const dateRaw = await prompt("Transfer date to update (YYYY-MM-DD): ");
  const feeValueRaw = await prompt("New fee value (number): ");

  const date = new Date(dateRaw);
  await db.collection("transfers").updateOne(
    { player_id: player._id, date },
    { $set: { fee_value: feeValueRaw ? parseFloat(feeValueRaw) : null } }
  );
  console.log("Transfer updated.");
}

async function deleteTransfer(db) {
  const player = await selectPlayer(db);
  if (!player) return;
  const dateRaw = await prompt("Transfer date to delete (YYYY-MM-DD): ");
  const date = new Date(dateRaw);
  await db.collection("transfers").deleteOne({ player_id: player._id, date });
  console.log("Transfer deleted.");
}

async function createMarketValue(db) {
  const player = await selectPlayer(db);
  if (!player) return;
  const dateRaw = await prompt("Market value date (YYYY-MM-DD): ");
  const valueRaw = await prompt("Market value number: ");
  const clubName = await prompt("Club name: ");

  const club = await db.collection("clubs").findOneAndUpdate(
    { name: clubName },
    { $set: { name: clubName } },
    { upsert: true, returnDocument: "after" }
  );

  await db.collection("market_values").insertOne({
    player_id: player._id,
    mv_date: new Date(dateRaw),
    mv_value: parseFloat(valueRaw),
    mv_unit: "€",
    mv_club_id: club.value._id
  });

  console.log("Market value created.");
}

async function readMarketValues(db) {
  const player = await selectPlayer(db);
  if (!player) return;
  const values = await db.collection("market_values")
    .find({ player_id: player._id })
    .sort({ mv_date: -1 })
    .limit(10)
    .toArray();
  console.table(values.map(v => ({ date: v.mv_date, value: v.mv_value, unit: v.mv_unit })));
}

async function createScoutReport(db, user) {
  const player = await selectPlayer(db);
  if (!player) return;
  const notes = await prompt("Notes: ");
  const tagsRaw = await prompt("Tags (comma separated): ");
  const technique = await prompt("Technique rating (1-10, blank to skip): ");
  const speed = await prompt("Speed rating (1-10, blank to skip): ");
  const mentality = await prompt("Mentality rating (1-10, blank to skip): ");

  await db.collection("scout_reports").insertOne({
    player_id: player._id,
    scout_user_id: user._id,
    notes,
    tags: tagsRaw ? tagsRaw.split(",").map(t => t.trim()).filter(Boolean) : [],
    ratings: {
      technique: technique ? parseInt(technique, 10) : null,
      speed: speed ? parseInt(speed, 10) : null,
      mentality: mentality ? parseInt(mentality, 10) : null
    },
    created_at: new Date()
  });

  console.log("Scout report created.");
}

async function updateScoutReport(db, user) {
  const reportId = await prompt("Report id: ");
  if (!ObjectId.isValid(reportId)) {
    console.log("Invalid report id.");
    return;
  }
  const notes = await prompt("New notes: ");
  await db.collection("scout_reports").updateOne(
    { _id: new ObjectId(reportId), scout_user_id: user._id },
    { $set: { notes } }
  );
  console.log("Scout report updated.");
}

async function deleteScoutReport(db, user) {
  const reportId = await prompt("Report id: ");
  if (!ObjectId.isValid(reportId)) {
    console.log("Invalid report id.");
    return;
  }
  await db.collection("scout_reports").deleteOne({ _id: new ObjectId(reportId), scout_user_id: user._id });
  console.log("Scout report deleted.");
}

async function readScoutReports(db) {
  const player = await selectPlayer(db);
  if (!player) return;
  const reports = await db.collection("scout_reports")
    .find({ player_id: player._id })
    .sort({ created_at: -1 })
    .limit(5)
    .toArray();
  console.table(reports.map(r => ({ id: r._id, notes: r.notes, tags: r.tags?.join(",") } )));
}

async function analystTopMarketValues(db) {
  const pipeline = [
    { $sort: { mv_date: -1 } },
    {
      $group: {
        _id: "$player_id",
        latest: { $first: "$mv_value" },
        date: { $first: "$mv_date" }
      }
    },
    { $sort: { latest: -1 } },
    { $limit: 10 }
  ];

  const results = await db.collection("market_values").aggregate(pipeline).toArray();
  const playerIds = results.map(r => r._id);
  const players = await db.collection("players").find({ _id: { $in: playerIds } }).toArray();
  const map = new Map(players.map(p => [p._id.toString(), p]));

  console.table(results.map(r => {
    const p = map.get(r._id.toString());
    return { player: p ? `${p.first_name} ${p.last_name}` : "", value: r.latest, date: r.date };
  }));
}

async function analystTopScorers(db) {
  const season = await prompt("Season code (e.g., 22/23): ");
  const league = await prompt("League name: ");
  const leagueDoc = await db.collection("leagues").findOne({ name: league });
  const seasonDoc = await db.collection("seasons").findOne({ code: season });
  if (!leagueDoc || !seasonDoc) {
    console.log("Season or league not found.");
    return;
  }

  const pipeline = [
    { $match: { season_id: seasonDoc._id, league_id: leagueDoc._id } },
    { $sort: { goal: -1 } },
    { $limit: 10 }
  ];

  const results = await db.collection("player_stats").aggregate(pipeline).toArray();
  const playerIds = results.map(r => r.player_id);
  const players = await db.collection("players").find({ _id: { $in: playerIds } }).toArray();
  const map = new Map(players.map(p => [p._id.toString(), p]));

  console.table(results.map(r => {
    const p = map.get(r.player_id.toString());
    return { player: p ? `${p.first_name} ${p.last_name}` : "", goals: r.goal };
  }));
}

async function analystTransferSpending(db) {
  const pipeline = [
    { $match: { fee_value: { $ne: null } } },
    {
      $group: {
        _id: "$new_club_id",
        total_spent: { $sum: "$fee_value" },
        deals: { $sum: 1 }
      }
    },
    { $sort: { total_spent: -1 } },
    { $limit: 10 }
  ];

  const results = await db.collection("transfers").aggregate(pipeline).toArray();
  const clubIds = results.map(r => r._id).filter(Boolean);
  const clubs = await db.collection("clubs").find({ _id: { $in: clubIds } }).toArray();
  const map = new Map(clubs.map(c => [c._id.toString(), c]));

  console.table(results.map(r => {
    const club = map.get(r._id?.toString?.() || "");
    return { club: club?.name || "Unknown", total_spent: r.total_spent, deals: r.deals };
  }));
}

async function adminMenu(db, user) {
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
  }
}

async function scoutMenu(db, user) {
  while (true) {
    console.log("\nScout Menu");
    console.log("1. Read player");
    console.log("2. Create scout report");
    console.log("3. Update scout report");
    console.log("4. Delete scout report");
    console.log("5. Read scout reports");
    console.log("0. Logout");

    const choice = await prompt("Choose: ");
    if (choice === "0") break;
    if (choice === "1") await runSafely(() => readPlayer(db));
    else if (choice === "2") await runSafely(() => createScoutReport(db, user));
    else if (choice === "3") await runSafely(() => updateScoutReport(db, user));
    else if (choice === "4") await runSafely(() => deleteScoutReport(db, user));
    else if (choice === "5") await runSafely(() => readScoutReports(db));
  }
}

async function analystMenu(db) {
  while (true) {
    console.log("\nAnalyst Menu");
    console.log("1. Read player");
    console.log("2. Top 10 market values (latest)");
    console.log("3. Top scorers by season/league");
    console.log("4. Transfer spending by club");
    console.log("0. Logout");

    const choice = await prompt("Choose: ");
    if (choice === "0") break;
    if (choice === "1") await runSafely(() => readPlayer(db));
    else if (choice === "2") await runSafely(() => analystTopMarketValues(db));
    else if (choice === "3") await runSafely(() => analystTopScorers(db));
    else if (choice === "4") await runSafely(() => analystTransferSpending(db));
  }
}

async function main() {
  const db = await getDb();

  console.log("\nCS 5200 Player Database CLI");
  console.log("Login with admin/scout/analyst.");

  const session = await login(db);
  if (!session) {
    console.log("Invalid credentials.");
    await closeDb();
    rl.close();
    return;
  }

  const { user, roles } = session;
  if (hasRole(roles, "admin")) {
    await adminMenu(db, user);
  } else if (hasRole(roles, "scout")) {
    await scoutMenu(db, user);
  } else if (hasRole(roles, "analyst")) {
    await analystMenu(db);
  } else {
    console.log("No role assigned.");
  }

  await closeDb();
  rl.close();
}

main().catch(err => {
  console.error(err);
  rl.close();
  process.exit(1);
});
