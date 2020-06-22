import sqlite3

from WiertarBot import config


class db():
    _conn: sqlite3.Connection = None

    @classmethod
    def get(self) -> sqlite3.Connection:
        if self._conn:
            return self._conn
        else:
            return self._connect(self)

    def _connect(self) -> sqlite3.Connection:
        self._conn = sqlite3.connect(config.db_path)
        self._create_tables(self)

        return self._conn

    def _create_tables(self):
        with open(config.db_schema_path, 'r') as schema:
            self._conn.cursor().executescript(schema.read())
