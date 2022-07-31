from pathlib import Path

root_dir = Path(__file__).parents[2]

db_path = root_dir / "db.sqlite3"
db_schema_path = root_dir / "WiertarBot/config/db_schema.sql"

config_path = root_dir / "application.yml"

upload_save_path = root_dir / "WiertarBot/upload"
attachment_save_path = root_dir / "saved"
cmd_media_path = root_dir / "WiertarBot/commands/media"

cookie_path = root_dir / "cookies.json"

for directory in [upload_save_path, attachment_save_path]:
    if not directory.exists():
        directory.mkdir()

image_edit_timeout = 5 * 60

time_to_remove_sent_messages = 24 * 60 * 60
