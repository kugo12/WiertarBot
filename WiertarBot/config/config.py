from os import path, mkdir

rootdir = path.dirname(__file__)
rootdir = path.join(rootdir, '../..')
rootdir = path.abspath(rootdir)

db_path = path.join(rootdir, 'db.sqlite3')
db_schema_path = path.join(rootdir, 'WiertarBot/config/db_schema.sql')

upload_save_path = path.join(rootdir, 'WiertarBot/upload')
if not path.exists(upload_save_path):
    mkdir(upload_save_path)

cookie_path = path.join(rootdir, 'cookies.json')

email = ''
password = ''

prefix = '!'
