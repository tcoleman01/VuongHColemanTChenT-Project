# CS 5200 Player Database (MongoDB + Node CLI)

## Quick Start
1. Install dependencies:

```bash
npm install
```

2. Make sure MongoDB is running locally.

3. Create a `.env` file from `.env.example`:

```bash
cp .env.example .env
```

4. Initialize the schema (creates collections + validators + indexes):

```bash
npm run init-schema
```

5. Seed the database from CSVs:

```bash
npm run seed
```

The seed script looks for data in:
- `DATA_DIR` env var (if provided)
- `./data` (fallback)

To make this portable on any machine, place the CSVs in `./data` or set `DATA_DIR` in your `.env`, for example:

```bash
DATA_DIR=/path/to/your/csvs
```

6. Run the CLI app:

```bash
npm start
```

### Demo Login
- `admin` / `admin123`
- `scout` / `scout123`
- `analyst` / `analyst123`

### What You Can Do
- **Admin**: CRUD for players, transfers, market values
- **Scout**: CRUD for scout reports + read players
- **Analyst**: Read players + analytics queries

## Optional: Create a MongoDB Dump
```bash
./scripts/dump.sh
```

## Features
- MongoDB schema with validation + indexes
- CLI CRUD operations (players, transfers, market values)
- Bonus #7 implemented: multiple user roles (admin, scout, analyst)
- Scout reports with embedded objects + arrays
- Analyst queries (aggregations)

## Files
- Schema + constraints: `schema/mongodb_schema.md`
- Schema initializer: `scripts/init-schema.js`
- Seed/ETL: `scripts/seed.js`
- CLI app: `src/cli.js`


