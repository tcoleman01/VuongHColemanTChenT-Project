# CS 5200 Player Database (MySQL + Java Admin CLI)

## What This Project Is
This is a Java command-line application that manages admin CRUD workflows for a soccer player database stored in MySQL.

## Java Admin CLI (Current)
1. Make sure MySQL is running and `.env` is configured.
2. Download MySQL JDBC Driver (Connector/J). Keep the jar outside the repo (to minimize files) and reference it by path.
3. Compile and run:

```bash
javac -cp "$HOME/Downloads/mysql-connector-j-<version>.jar" AdminCli.java
java -cp ".:$HOME/Downloads/mysql-connector-j-<version>.jar" AdminCli
```

On Windows:

```bash
java -cp .;%HOMEPATH%\\Downloads\\mysql-connector-j-<version>.jar AdminCli
```

Optional credentials (if you want to lock login):

```
ADMIN_USER=admin
ADMIN_PASS=admin123
USER_USER=user
USER_PASS=user123
```

## Loading the Database (Run These in Order)

From your terminal, `cd` into the project root first:

```bash
cd /path/to/VuongHColemanTChenT-Project
```

**Step 1 — Create tables:**
```bash
mysql -u root -p < soccer_analytics_db_schema.sql
```

**Step 2 — Load stored procedures and triggers:**
```bash
mysql -u root -p soccer_analytics_db < soccer_analytics_db_objects.sql
```
> If this fails on `DELIMITER`, use `SOURCE` instead (see Troubleshooting below).

**Step 3 — Enable local file loading (required once per MySQL install):**
```bash
mysql -u root -p -e "SET GLOBAL local_infile = 1;"
```

**Step 4 — Load CSV data:**
```bash
mysql -u root -p --local-infile=1 < soccer_analytics_db_loader.sql
```

### Troubleshooting

**`DELIMITER` error on step 2** — use the MySQL `SOURCE` command instead:
```bash
mysql -u root -p
```
Then inside the MySQL prompt:
```sql
USE soccer_analytics_db;
SOURCE /full/path/to/VuongHColemanTChenT-Project/soccer_analytics_db_objects.sql;
```

**`Loading local data is disabled` error on step 4** — make sure you ran step 3 first, and that you included `--local-infile=1` in the step 4 command.

---

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

## Core Files (Where to Look)
- Java admin CLI: `AdminCli.java`
- Schema + constraints: `soccer_analytics_db.sql`
- Schema overview: `schema/mysql_schema.md`
