import sqlite3
from pathlib import Path

db_directory = Path(__file__).parents[1]

db_path = db_directory / "db.sqlite3"

conn = sqlite3.connect(str(db_path))
cur = conn.cursor()

cur.executescript("""
CREATE TABLE permission(
    id INTEGER NOT NULL PRIMARY KEY,
    command VARCHAR(255) NOT NULL,
    whitelist TEXT NOT NULL,
    blacklist TEXT NOT NULL
);
CREATE UNIQUE INDEX permission_command ON permission (command);

CREATE TABLE fbmessage(
    id INTEGER NOT NULL PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL,
    thread_id VARCHAR(255) NOT NULL,
    author_id VARCHAR(255) NOT NULL,
    time INTEGER NOT NULL,
    message TEXT NOT NULL,
    deleted_at INTEGER
);
CREATE UNIQUE INDEX fbmessage_message_id ON fbmessage (message_id);


INSERT INTO permission(id, command, whitelist, blacklist)
SELECT ROW_NUMBER() over () AS id, command, whitelist, blacklist FROM permissions;

INSERT INTO fbmessage (id, message_id, thread_id, author_id, time, message, deleted_at) 
SELECT row_number() over () AS id, mid, thread_id, author_id, time, message, deleted_at
FROM (
    SELECT mid, thread_id, author_id, time, message, NULL AS deleted_at
    FROM messages
    WHERE mid NOT IN (SELECT dm.mid FROM deleted_messages dm)
    GROUP BY mid
    
    UNION ALL
    
    SELECT mid, thread_id, author_id, time, message, deleted_at
    FROM deleted_messages
    GROUP BY mid
);


DROP TABLE permissions;
DROP TABLE messages;
DROP TABLE deleted_messages;
DROP TABLE deleted_notes;
DROP TABLE notes;
""")

conn.commit()
conn.close()
