import { getDb, closeDb } from "../src/db.js";

const collections = [
  "players",
  "clubs",
  "leagues",
  "seasons",
  "player_stats",
  "market_values",
  "transfers",
  "countries",
  "users",
  "roles",
  "scout_reports"
];

const validators = {
  players: {
    $jsonSchema: {
      bsonType: "object",
      required: ["first_name", "last_name", "dob", "position", "club_id"],
      properties: {
        first_name: { bsonType: "string" },
        last_name: { bsonType: "string" },
        dob: { bsonType: "date" },
        name_in_home_country: { bsonType: ["string", "null"] },
        place_of_birth: { bsonType: ["string", "null"] },
        height_cm: { bsonType: ["int", "null"] },
        position: { bsonType: "string" },
        foot: { bsonType: ["string", "null"] },
        club_id: { bsonType: "objectId" },
        join_date: { bsonType: ["date", "null"] },
        contract_expires: { bsonType: ["date", "null"] },
        profile: {
          bsonType: "object",
          properties: {
            citizenships: { bsonType: "array", items: { bsonType: "string" } }
          }
        }
      }
    }
  },
  clubs: {
    $jsonSchema: {
      bsonType: "object",
      required: ["name"],
      properties: {
        name: { bsonType: "string" },
        country: { bsonType: ["string", "null"] }
      }
    }
  },
  leagues: {
    $jsonSchema: {
      bsonType: "object",
      required: ["name"],
      properties: {
        name: { bsonType: "string" },
        level: { bsonType: ["int", "null"] }
      }
    }
  },
  seasons: {
    $jsonSchema: {
      bsonType: "object",
      required: ["code"],
      properties: {
        code: { bsonType: "string" }
      }
    }
  },
  player_stats: {
    $jsonSchema: {
      bsonType: "object",
      required: ["player_id", "season_id", "league_id"],
      properties: {
        player_id: { bsonType: "objectId" },
        season_id: { bsonType: "objectId" },
        league_id: { bsonType: "objectId" },
        appear: { bsonType: ["int", "null"] },
        goal: { bsonType: ["int", "null"] },
        assist: { bsonType: ["int", "null"] },
        play_time: { bsonType: ["int", "null"] }
      }
    }
  },
  market_values: {
    $jsonSchema: {
      bsonType: "object",
      required: ["player_id", "mv_date", "mv_value", "mv_unit"],
      properties: {
        player_id: { bsonType: "objectId" },
        mv_date: { bsonType: "date" },
        mv_value: { bsonType: "double" },
        mv_unit: { bsonType: "string" },
        mv_club_id: { bsonType: ["objectId", "null"] }
      }
    }
  },
  transfers: {
    $jsonSchema: {
      bsonType: "object",
      required: ["player_id", "date"],
      properties: {
        player_id: { bsonType: "objectId" },
        date: { bsonType: "date" },
        fee_value: { bsonType: ["double", "null"] },
        fee_text: { bsonType: ["string", "null"] },
        old_club_id: { bsonType: ["objectId", "null"] },
        new_club_id: { bsonType: ["objectId", "null"] }
      }
    }
  },
  countries: {
    $jsonSchema: {
      bsonType: "object",
      required: ["name"],
      properties: {
        name: { bsonType: "string" }
      }
    }
  },
  users: {
    $jsonSchema: {
      bsonType: "object",
      required: ["username", "password_hash", "role_ids"],
      properties: {
        username: { bsonType: "string" },
        password_hash: { bsonType: "string" },
        role_ids: { bsonType: "array", items: { bsonType: "objectId" } }
      }
    }
  },
  roles: {
    $jsonSchema: {
      bsonType: "object",
      required: ["name"],
      properties: {
        name: { bsonType: "string" },
        permissions: { bsonType: "array", items: { bsonType: "string" } }
      }
    }
  },
  scout_reports: {
    $jsonSchema: {
      bsonType: "object",
      required: ["player_id", "scout_user_id", "notes", "created_at"],
      properties: {
        player_id: { bsonType: "objectId" },
        scout_user_id: { bsonType: "objectId" },
        notes: { bsonType: "string" },
        tags: { bsonType: "array", items: { bsonType: "string" } },
        ratings: {
          bsonType: "object",
          properties: {
            technique: { bsonType: ["int", "null"] },
            speed: { bsonType: ["int", "null"] },
            mentality: { bsonType: ["int", "null"] }
          }
        },
        created_at: { bsonType: "date" }
      }
    }
  }
};

async function ensureCollections(db) {
  const existing = new Set((await db.listCollections().toArray()).map(c => c.name));

  for (const name of collections) {
    const validator = validators[name] || undefined;
    if (!existing.has(name)) {
      await db.createCollection(name, validator ? { validator } : undefined);
    } else if (validator) {
      await db.command({ collMod: name, validator });
    }
  }

  await db.collection("players").createIndex(
    { first_name: 1, last_name: 1, dob: 1 },
    { unique: true, name: "player_identity_unique" }
  );
  await db.collection("clubs").createIndex({ name: 1 }, { unique: true });
  await db.collection("leagues").createIndex({ name: 1 }, { unique: true });
  await db.collection("seasons").createIndex({ code: 1 }, { unique: true });
  await db.collection("countries").createIndex({ name: 1 }, { unique: true });
  await db.collection("roles").createIndex({ name: 1 }, { unique: true });
  await db.collection("users").createIndex({ username: 1 }, { unique: true });
  await db.collection("player_stats").createIndex({ player_id: 1, season_id: 1, league_id: 1 }, { unique: true });
  await db.collection("market_values").createIndex({ player_id: 1, mv_date: 1 });
  await db.collection("transfers").createIndex({ player_id: 1, date: 1 });
}

async function main() {
  const db = await getDb();
  if (process.env.RESET_DB === "true") {
    await db.dropDatabase();
  }
  await ensureCollections(db);
  await closeDb();
  console.log("Schema initialized.");
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
