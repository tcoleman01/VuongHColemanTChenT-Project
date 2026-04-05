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

async function main() {
  const sql = loadSchemaSql();
  const connection = await mysql.createConnection(config);
  await connection.query(sql);
  await connection.end();
  console.log("Schema initialized.");
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
