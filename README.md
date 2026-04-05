# CS 5200 Player Database (MySQL + Node CLI)

## What This Project Is
This is a Node.js command-line application that manages a soccer player database stored in MySQL. Users choose a role in the CLI and get role-based menus for CRUD workflows and analytics queries.

## Quick Start (Portable Setup)
1. Install dependencies:

```bash
npm install
```

2. Make sure MySQL is running.

3. Create a `.env` file from `.env.example`:

```bash
cp .env.example .env
```

4. Update your `.env` with MySQL credentials.

Example `.env`:
```
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=Root123
MYSQL_DATABASE=soccer_analytics_db
```
Note: This password is for local development only. Do not commit real passwords.

5. Initialize the schema (tables + constraints) from `soccer_analytics_db.sql`:

```bash
npm run init-schema
```

6. Seed the database with sample data:

```bash
npm run seed
```

7. (Submission) Create a self-contained MySQL dump (DDL + DML + routines):

```bash
bash scripts/dump.sh
```

8. Run the CLI app:

```bash
npm start
```

### Role Selection
- `admin`
- `analyst`

## MySQL Setup Checklist
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

## Role Capabilities
- **Admin**: CRUD for players, transfers, market values
- **Analyst**: Read players + analytics queries

## Core Files (Where to Look)
- CLI app: `src/cli.js`
- Database connection: `src/db.js`
- Schema + constraints: `soccer_analytics_db.sql`
- Schema overview: `schema/mysql_schema.md`
- Schema initializer: `scripts/init-schema.js`
- Seed data: `scripts/seed.js`
- MySQL dump script: `scripts/dump.sh`

## Where to Build On (Detailed)
If you want to extend the project, the following are the most stable and intentional extension points:

### 1) Add New CLI Actions
File: `src/cli.js`

Pattern:
Add a new menu item in `adminMenu` or `analystMenu`.
Create a new `async function` that collects input with `prompt(...)`.
Query or update MySQL via `db.execute(...)`.
Print results with `console.log` or `console.table`.

Example steps:
1. Add a new menu choice number.
2. Add `else if (choice === "X") await runSafely(() => yourFunction(db));`.
3. Implement `yourFunction`.

### 2) Update the Schema
File: `soccer_analytics_db.sql`

Steps:
1. Add or modify tables and constraints.
2. Re-run:
```bash
npm run init-schema
```

### 3) Seed New Data
File: `scripts/seed.js`

Steps:
1. Insert new rows with `db.execute(...)`.
2. Keep inserts idempotent by checking for existing rows before insert.

Then run:
```bash
npm run seed
```

### 4) Add Analytics Queries
File: `src/cli.js`

Use SQL queries:
- Write a SQL query with `JOIN`, `GROUP BY`, and `ORDER BY`.
- Call `db.execute(query, params)`.

## Features
- MySQL schema with constraints
- Schema overview: `schema/mysql_schema.md`
- CLI CRUD operations (players, transfers, market values)
- Role-based menus (admin, analyst)
- Analyst queries using SQL
