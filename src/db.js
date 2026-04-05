import mysql from "mysql2/promise";
import dotenv from "dotenv";

dotenv.config();

const config = {
  host: process.env.MYSQL_HOST || "localhost",
  port: process.env.MYSQL_PORT ? Number(process.env.MYSQL_PORT) : 3306,
  user: process.env.MYSQL_USER || "root",
  password: process.env.MYSQL_PASSWORD || "",
  database: process.env.MYSQL_DATABASE || "soccer_analytics_db"
};

let pool;

export async function getDb() {
  if (!pool) {
    pool = mysql.createPool({
      ...config,
      connectionLimit: 10
    });
  }
  return pool;
}

export async function closeDb() {
  if (pool) {
    await pool.end();
    pool = null;
  }
}
