BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS "notes" (
	"id"	INTEGER PRIMARY KEY AUTOINCREMENT,
	"mid"	TEXT,
	"uid"	TEXT,
	"tid"	TEXT,
	"start"	TEXT,
	"end"	TEXT,
	"note"	TEXT
);
CREATE TABLE IF NOT EXISTS "deleted_notes" (
	"id"	TEXT,
	"mid"	TEXT,
	"uid"	TEXT,
	"tid"	TEXT,
	"start"	TEXT,
	"end"	TEXT,
	"note"	TEXT
);
CREATE TABLE IF NOT EXISTS "messages" (
	"mid"	TEXT,
	"thread_id"	TEXT,
	"author_id"	TEXT,
	"time"	TEXT,
	"message"	TEXT
);
CREATE TABLE IF NOT EXISTS "deleted_messages" (
	"mid"	TEXT,
	"thread_id"	TEXT,
	"author_id"	TEXT,
	"time"	TEXT,
	"deleted_at"	TEXT,
	"message"	TEXT
);
CREATE TABLE IF NOT EXISTS "permissions" (
	"command"	TEXT,
	"whitelist"	TEXT,
	"blacklist"	TEXT
);
COMMIT;