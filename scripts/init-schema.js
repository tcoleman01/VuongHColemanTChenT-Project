import fs from "fs";
import path from "path";
import mysql from "mysql2/promise";
import dotenv from "dotenv";

dotenv.config();

const config = {
  host: process.env.MYSQL_HOST || "localhost",
  port: process.env.MYSQL_PORT ? Number(process.env.MYSQL_PORT) : 3306,
  user: process.env.MYSQL_USER || "root",
  password: process.env.MYSQL_PASSWORD || "",
  multipleStatements: true
};

function loadSchemaSql() {
  const filePath = path.resolve(process.cwd(), "soccer_analytics_db.sql");
  return fs.readFileSync(filePath, "utf-8");
}

function splitSqlStatements(sql) {
  const statements = [];
  let delimiter = ";";
  let buffer = "";

  const lines = sql.split(/\r?\n/);
  for (const rawLine of lines) {
    const line = rawLine.trimEnd();
    const delimiterMatch = line.match(/^DELIMITER\s+(.+)$/i);
    if (delimiterMatch) {
      delimiter = delimiterMatch[1].trim();
      continue;
    }

    buffer += rawLine + "\n";

    if (buffer.trimEnd().endsWith(delimiter)) {
      const statement = buffer.trim();
      const trimmed = statement.slice(0, -delimiter.length).trim();
      if (trimmed.length > 0) {
        statements.push(trimmed);
      }
      buffer = "";
    }
  }

  const remaining = buffer.trim();
  if (remaining.length > 0) {
    statements.push(remaining);
  }

  return statements;
}

async function main() {
  const sql = loadSchemaSql();
  const connection = await mysql.createConnection(config);
  const statements = splitSqlStatements(sql);
  for (const statement of statements) {
    await connection.query(statement);
  }
  await connection.end();
  console.log("Schema initialized.");
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
