# Project Guide: CS 5200 Player Database (MySQL + Node CLI)

This guide is designed for teammates who are **new to JavaScript** and need a **detailed walkthrough** of how this repository works and where to extend it.

---

## 1) What This Project Is
This is a **Node.js command-line application** that manages a **soccer player database** stored in **MySQL**.

The program flow is:
1. Connect to MySQL
2. Prompt the user to choose a role
3. Show a role-specific menu (Admin / Analyst)
4. Run CRUD actions or analytics queries

The app is intentionally simple:
- **No web UI**
- **No server routes**
- Just a CLI + scripts + MySQL

---

## 2) Quick Start (Portable Setup)

### Prerequisites
- Node.js (LTS)
- MySQL running (local or remote)

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
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=Root123
MYSQL_DATABASE=soccer_analytics_db
```
Note: This password is for local development only. Do not commit real passwords.

### Initialize schema (tables + constraints)
```bash
npm run init-schema
```

### Seed the database with sample data
```bash
npm run seed
```

### Run the CLI
```bash
npm start
```

### Role selection
- `admin`
- `analyst`

---

## 3) MySQL Setup Checklist
Use this if you’re setting up MySQL on a new machine.

1. Install MySQL.
macOS (Homebrew): `brew install mysql`
Windows: MySQL Installer
Linux: `sudo apt-get install mysql-server`

2. Start MySQL.
macOS: `brew services start mysql`
Linux: `sudo systemctl start mysql`

3. Set or confirm the root password.
If you can log in: `mysql -u root -p`
Once inside MySQL:
```sql
ALTER USER 'root'@'localhost' IDENTIFIED BY 'Root123';
FLUSH PRIVILEGES;
```

4. Enable the event scheduler (required for scheduled events).
```sql
SET GLOBAL event_scheduler = ON;
```

5. Create the database (optional, script also creates it).
```sql
CREATE DATABASE IF NOT EXISTS soccer_analytics_db;
```

6. Confirm you can connect.
```bash
mysql -u root -p
```

---

## 4) Core Files (Where Things Live)
- CLI app: `src/cli.js`
- DB connection: `src/db.js`
- Schema: `soccer_analytics_db.sql`
- Schema overview: `schema/mysql_schema.md`
- Schema initializer: `scripts/init-schema.js`
- Seed data: `scripts/seed.js`
- MySQL dump script: `scripts/dump.sh`

---

## 5) JavaScript Basics (Enough to Read This Repo)

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
  const [rows] = await db.execute("SELECT * FROM Player");
  return rows;
}
```

### Objects
```js
const player = {
  first_name: "Lionel",
  last_name: "Messi",
  club_id: 1
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

## 6) How the CLI Works (Code Walkthrough)

The CLI is in `src/cli.js`.

### The main loop
```js
async function main() {
  const db = await getDb();
  const role = await chooseRole();
  ...
}
```

### Role routing
```js
if (role === "admin") {
  await adminMenu(db);
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

## 7) MySQL Basics (Used Everywhere)

### Run a query
```js
const [rows] = await db.execute("SELECT * FROM Player WHERE last_name = ?", ["Messi"]);
```

### Insert
```js
await db.execute(
  "INSERT INTO Player (first_name, last_name, dob) VALUES (?, ?, ?)",
  ["Lionel", "Messi", "1987-06-24"]
);
```

### Update
```js
await db.execute(
  "UPDATE Player SET club_id = ? WHERE player_id = ?",
  [newClubId, playerId]
);
```

### Delete
```js
await db.execute(
  "DELETE FROM Player WHERE player_id = ?",
  [playerId]
);
```

---

## 8) Where to Build On (Detailed)

### A) Add New CLI Actions (Most Common Extension)
File: `src/cli.js`

Steps:
1. Add a new menu option (number + text).
2. Add a handler: `else if (choice === "X") await runSafely(() => yourFunction(db));`.
3. Implement the function to prompt for input, query or update MySQL, and print output.

**Example pattern:**
```js
async function createExample(db) {
  const name = await prompt("Name: ");
  await db.execute("INSERT INTO Example (name) VALUES (?)", [name]);
  console.log("Example created.");
}
```

---

### B) Update the Schema
File: `soccer_analytics_db.sql`

Steps:
1. Add or modify tables and constraints.
2. Run:
```bash
npm run init-schema
```

---

### C) Seed New Data
File: `scripts/seed.js`

Steps:
1. Insert rows using `db.execute(...)`.
2. Keep inserts idempotent by checking for existing rows before insert.

Run:
```bash
npm run seed
```

---

### D) Create a Submission Dump (DDL + DML + routines)
File: `scripts/dump.sh`

This generates a single SQL file that contains the schema, data, and database objects.

Run:
```bash
bash scripts/dump.sh
```

The dump will be created at `dump/soccer_analytics_db_dump.sql`.

---

### E) Add Analytics Queries
File: `src/cli.js`

Use SQL queries:
- Write a SQL query with `JOIN`, `GROUP BY`, and `ORDER BY`.
- Call `db.execute(query, params)`.

---

## 9) Data Structure Orientation
High-level tables:

**Entities**
- `Country`, `League`, `Stadium`, `Club`, `Coach`, `Position`, `Player`

**Stats / Events**
- `MarketValue`, `Transfer`, `Match`

Relationships are stored as foreign keys.

---

## 10) Summary
This repository is designed to be extendable through:
- new CLI actions
- schema changes
- new seed data
- new analytics reports

The main implementation files are `src/cli.js` and `scripts/`.
