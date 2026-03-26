import { MongoClient } from "mongodb";
import dotenv from "dotenv";

dotenv.config();

const uri = process.env.MONGO_URI || "mongodb://localhost:27017";
const dbName = process.env.DB_NAME || "cs5200_player_db";

let client;

export async function getDb() {
  if (!client) {
    client = new MongoClient(uri);
    await client.connect();
  }
  return client.db(dbName);
}

export async function closeDb() {
  if (client) {
    await client.close();
    client = null;
  }
}
