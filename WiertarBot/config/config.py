from os import path

rootdir = path.dirname(__file__)
rootdir = path.join(rootdir, '../..')
rootdir = path.abspath(rootdir)

db_path = path.join(rootdir, 'db.sqlite3')
