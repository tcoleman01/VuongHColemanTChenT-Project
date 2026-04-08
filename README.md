# CS 5200 Player Database (MySQL + Java Admin CLI)

## What This Project Is
This is a Java command-line application that manages admin CRUD workflows for a soccer player database stored in MySQL.

## Java Admin CLI (Current)
1. Make sure MySQL is running and `.env` is configured.
2. Download MySQL JDBC Driver (Connector/J) and place the jar in `lib/`.
3. Compile and run:

```bash
javac -cp lib/mysql-connector-j-<version>.jar AdminCli.java
java -cp .:lib/mysql-connector-j-<version>.jar AdminCli
```

On Windows:

```bash
java -cp .;lib/mysql-connector-j-<version>.jar AdminCli
```

Optional admin credentials (if you want to lock login):

```
ADMIN_USER=admin
ADMIN_PASS=admin123
```

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
