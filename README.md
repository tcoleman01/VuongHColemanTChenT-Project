# CS 5200 Player Database (MongoDB + Node CLI)

## What This Project Is
This is a Node.js command-line application that manages a soccer player database stored in MongoDB. Users log in via the CLI and get role-based menus for CRUD workflows and analytics queries.

## Quick Start (Portable Setup)
1. Install dependencies:

```bash
npm install
```

2. Make sure MongoDB is running.

3. Create a `.env` file from `.env.example`:

```bash
cp .env.example .env
```

4. Initialize the schema (collections + validators + indexes):

```bash
npm run init-schema
```

5. Seed the database from CSVs:

```bash
npm run seed
```

The seed script looks for data in:
- `DATA_DIR` env var (if provided and exists)
- `./data` (fallback)

To make this portable on any machine:
- Put the CSVs in `./data`, or
- Set `DATA_DIR=/absolute/path/to/your/csvs` in `.env`

6. Run the CLI app:

```bash
npm start
```

### Demo Login
- `admin` / `admin123`
- `scout` / `scout123`
- `analyst` / `analyst123`

## Role Capabilities
- **Admin**: CRUD for players, transfers, market values
- **Scout**: CRUD for scout reports + read players
- **Analyst**: Read players + analytics queries

## Core Files (Where to Look)
- CLI app: `src/cli.js`
- Database connection: `src/db.js`
- Schema + constraints: `schema/mongodb_schema.md`
- Schema initializer: `scripts/init-schema.js`
- Seed/ETL: `scripts/seed.js`

## Where to Build On (Detailed)
If you want to extend the project, the following are the most stable and intentional extension points:

### 1) Add New CLI Actions
File: `src/cli.js`

Pattern:
- Add a new menu item in `adminMenu`, `scoutMenu`, or `analystMenu`
- Create a new `async function` that:
  - collects input with `prompt(...)`
  - queries or updates the DB via `db.collection("...")`
  - prints results with `console.log` or `console.table`

Example steps:
- Add menu choice number
- Add `else if (choice === "X") await runSafely(() => yourFunction(db, user));`
- Implement `yourFunction`

### 2) Add a New Collection + Validation
File: `scripts/init-schema.js`

Steps:
- Add the collection name to `collections`
- Add a JSON Schema validator to `validators`
- Add indexes as needed in `ensureCollections`

Then run:
```bash
npm run init-schema
```

### 3) Seed New Data Sources
File: `scripts/seed.js`

Steps:
- Add new CSV path(s)
- Write a transform from CSV row -> MongoDB document
- Use `updateOne` + `{ upsert: true }` to keep the script idempotent

Then run:
```bash
npm run seed
```

### 4) Add Analytics Queries
File: `src/cli.js`

Use MongoDB aggregation pipelines:
- Build a `pipeline` array
- Call `db.collection("...").aggregate(pipeline).toArray()`
- Fetch any related collections (players/clubs) to display readable names

### 5) Add or Modify Roles
Files: `scripts/seed.js`, `src/cli.js`

Steps:
- Add or modify roles in `seedRolesAndUsers`
- Add a new menu function for the role
- Update `main()` role routing in `src/cli.js`

## Optional: Create a MongoDB Dump
```bash
./scripts/dump.sh
```

## Features
- MongoDB schema with validation + indexes
- CLI CRUD operations (players, transfers, market values)
- Multiple user roles (admin, scout, analyst)
- Scout reports with embedded objects + arrays
- Analyst queries (aggregations)

