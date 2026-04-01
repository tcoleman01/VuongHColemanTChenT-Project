# Project Guide: CS 5200 Player Database (MongoDB + Node CLI)

This guide is designed for teammates who are **new to JavaScript** and need a **detailed walkthrough** of how this repository works and where to extend it.

---

## 1) What This Project Is
This is a **Node.js command-line application** that manages a **soccer player database** stored in **MongoDB**.

The program flow is:
1. Connect to MongoDB
2. Prompt the user to log in
3. Load user roles
4. Show a role‑specific menu (Admin / Scout / Analyst)
5. Run CRUD actions or analytics queries

The app is intentionally simple:
- **No web UI**
- **No server routes**
- Just a CLI + scripts + MongoDB

---

## 2) Quick Start (Portable Setup)

### Prerequisites
- Node.js (LTS)
- MongoDB running (local or remote)

### Install dependencies
```bash
npm install
```

### Create `.env`
```bash
cp .env.example .env
```

Example `.env`:
```
MONGO_URI=mongodb://localhost:27017
DB_NAME=cs5200_player_db
```

### Initialize schema (collections + validators + indexes)
```bash
npm run init-schema
```

### Seed the database from CSVs
```bash
npm run seed
```

Seed data is loaded from:
- `DATA_DIR` if defined and valid
- otherwise `./data`

To point at a custom CSV folder:
```
DATA_DIR=/absolute/path/to/your/csvs
```

### Run the CLI
```bash
npm start
```

### Demo logins
- `admin / admin123`
- `scout / scout123`
- `analyst / analyst123`

---

## 3) Core Files (Where Things Live)
- CLI app: `src/cli.js`
- DB connection: `src/db.js`
- Schema overview: `schema/mongodb_schema.md`
- Schema initializer: `scripts/init-schema.js`
- Seed/ETL: `scripts/seed.js`

---

## 4) JavaScript Basics (Enough to Read This Repo)

### Imports
```js
import { getDb } from "./db.js";
```
This loads a function from another file.

### Variables
```js
const name = "Messi";
let age = 36;
```
- `const` means it won’t be reassigned
- `let` means it can change

### Functions
```js
function add(a, b) {
  return a + b;
}
```

### Async + Await
Most DB calls are async:
```js
async function getPlayers() {
  const players = await db.collection("players").find().toArray();
  return players;
}
```

### Objects
```js
const player = {
  first_name: "Lionel",
  last_name: "Messi",
  club_id: someObjectId
};
```

### Arrays
```js
const tags = ["fast", "technical", "leader"];
```

### Conditions
```js
if (choice === "1") {
  ...
} else if (choice === "2") {
  ...
}
```

---

## 5) How the CLI Works (Code Walkthrough)

The CLI is in `src/cli.js`.

### The main loop
```js
async function main() {
  const db = await getDb();
  const session = await login(db);
  ...
}
```

### Login flow
```js
const user = await db.collection("users").findOne({ username });
const ok = bcrypt.compareSync(password, user.password_hash);
```

### Role routing
```js
if (hasRole(roles, "admin")) {
  await adminMenu(db, user);
}
```

### Menus
Each menu is a `while(true)` loop:
```js
while (true) {
  console.log("1. Create player");
  ...
  const choice = await prompt("Choose: ");
  ...
}
```

---

## 6) MongoDB Basics (Used Everywhere)

### Collections
Each “table” is a collection:
```js
db.collection("players")
```

### Find one document
```js
await db.collection("players").findOne({ last_name: "Messi" })
```

### Find many documents
```js
await db.collection("players").find({ position: "MF" }).toArray()
```

### Insert
```js
await db.collection("players").insertOne(doc)
```

### Update
```js
await db.collection("players").updateOne(
  { _id: player._id },
  { $set: { club_id: newClubId } }
)
```

### Delete
```js
await db.collection("players").deleteOne({ _id: player._id })
```

---

## 7) Where to Build On (Detailed)

### A) Add New CLI Actions (Most Common Extension)
File: `src/cli.js`

Steps:
1. Add a new menu option (number + text)
2. Add a handler:
   ```js
   else if (choice === "X") await runSafely(() => yourFunction(db, user));
   ```
3. Implement the function:
   - prompt for input
   - query or update MongoDB
   - print output

**Example pattern:**
```js
async function createExample(db) {
  const name = await prompt("Name: ");
  await db.collection("examples").insertOne({ name });
  console.log("Example created.");
}
```

---

### B) Add a New Collection + Validation
File: `scripts/init-schema.js`

Steps:
1. Add collection name to `collections`
2. Add validator under `validators`
3. Add indexes in `ensureCollections`

Run:
```bash
npm run init-schema
```

---

### C) Seed New Data Sources
File: `scripts/seed.js`

Steps:
1. Add CSV path
2. `readCsv(...)`
3. Transform each row
4. Use `updateOne(..., { upsert: true })`

Run:
```bash
npm run seed
```

---

### D) Add Analytics Queries
File: `src/cli.js`

Use aggregation pipelines:
```js
const pipeline = [
  { $match: { ... } },
  { $group: { _id: "$player_id", total: { $sum: "$goal" } } },
  { $sort: { total: -1 } }
];
const results = await db.collection("player_stats").aggregate(pipeline).toArray();
```

---

### E) Add or Modify Roles
Files: `scripts/seed.js`, `src/cli.js`

Steps:
1. Add role in `seedRolesAndUsers`
2. Create a new menu function
3. Update `main()` to route that role

---

## 8) Data Structure Orientation
High‑level collections:

**Entities**
- `players`, `clubs`, `leagues`, `seasons`, `countries`

**Stats / Events**
- `player_stats`, `market_values`, `transfers`

**Auth**
- `users`, `roles`

**Reports**
- `scout_reports`

Relationships are stored as `ObjectId` references.

---

## 9) Summary
This repository is designed to be extendable through:
- new CLI actions
- new collections + validators
- new seed data
- new analytics reports
- new roles

The main implementation files are `src/cli.js` and `scripts/`.
