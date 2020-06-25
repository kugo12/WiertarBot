from os import path, mkdir

rootdir = path.dirname(__file__)
rootdir = path.join(rootdir, '../..')
rootdir = path.abspath(rootdir)

db_path = path.join(rootdir, 'db.sqlite3')
db_schema_path = path.join(rootdir, 'WiertarBot/config/db_schema.sql')

upload_save_path = path.join(rootdir, 'WiertarBot/upload')
attachment_save_path = path.join(rootdir, 'saved')

cookie_path = path.join(rootdir, 'cookies.json')

for directory in [upload_save_path, attachment_save_path]:
    if not path.exists(directory):
        mkdir(directory)

email = ''
password = ''

prefix = '!'
