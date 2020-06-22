from os import path

rootdir = path.dirname(__file__)
rootdir = path.join(rootdir, '../..')
rootdir = path.abspath(rootdir)

db_path = path.join(rootdir, 'db.sqlite3')
db_schema_path = path.join(rootdir, 'WiertarBot/config/db_schema.sql')
