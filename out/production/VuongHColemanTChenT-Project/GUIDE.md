# Project Guide: CS 5200 Player Database (MySQL + Java Admin CLI)

This guide is focused on the **Java admin CLI** (current direction). The older Node CLI remains in the repo as legacy reference only.

---

## 1) What This Project Is
This is a **Java command-line application** that manages **admin CRUD workflows** for a **soccer player database** stored in **MySQL**.

The Java app flow is:
1. Connect to MySQL
2. Prompt admin to log in
3. Show admin menu
4. Call stored procedures for CRUD actions

The app is intentionally simple:
- No web UI
- No server routes
- Just a Java CLI + MySQL

---

## 2) Quick Start (Java Admin CLI)

### Prerequisites
- Java 17+ (Java 11+ is fine)
- MySQL running (local or remote)
- MySQL JDBC Driver (Connector/J) jar in `lib/`

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

Optional admin credentials (if you want to lock login):
```
ADMIN_USER=admin
ADMIN_PASS=admin123
```

### Compile + Run
```bash
javac -cp lib/mysql-connector-j-<version>.jar AdminCli.java
java -cp .:lib/mysql-connector-j-<version>.jar AdminCli
```

On Windows:
```bash
java -cp .;lib/mysql-connector-j-<version>.jar AdminCli
```

---

## 3) MySQL Setup Checklist
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
- Java admin CLI: `AdminCli.java`
- Schema: `soccer_analytics_db.sql`
- Schema overview: `schema/mysql_schema.md`

---

## 5) Java Admin CLI Walkthrough
The CLI is in `AdminCli.java`.

Key points:
- Reads DB config from `.env` or environment variables.
- Prompts for admin login.
- Uses JDBC `CallableStatement` to call stored procedures.
- Includes a custom procedure option so you can wire in new DB objects quickly.

---

## 6) Legacy Node CLI
The Node CLI was removed as part of the Java migration to reduce redundancy.
