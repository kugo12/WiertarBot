from pathlib import Path
from os import getcwd

root_dir = Path(getcwd())

config_path = root_dir / "application.yml"

_runtime_data = root_dir / "data"
upload_save_path = _runtime_data / "upload"
attachment_save_path = _runtime_data / "saved"
cmd_media_path = _runtime_data / "media"

cookie_path = _runtime_data / "cookies.json"

for directory in (upload_save_path, attachment_save_path):
    if not directory.exists():
        directory.mkdir()

image_edit_timeout = 5 * 60

time_to_remove_sent_messages = 24 * 60 * 60
