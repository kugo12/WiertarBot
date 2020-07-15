import sqlite3

from WiertarBot import config


class db():
    _conn: sqlite3.Connection = None

    @classmethod
    def get(cls) -> sqlite3.Connection:
        if cls._conn:
            return cls._conn
        else:
            return cls._connect()

    @classmethod
    def _connect(cls) -> sqlite3.Connection:
        cls._conn = sqlite3.connect(config.db_path)
        cls._create_tables()

        return cls._conn

    @classmethod
    def _create_tables(cls):
        with open(config.db_schema_path, 'r') as schema:
            cls._conn.cursor().executescript(schema.read())
