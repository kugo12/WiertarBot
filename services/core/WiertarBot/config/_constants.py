from pathlib import Path
from os import getcwd

root_dir = Path(getcwd())

_runtime_data = root_dir / "data"
attachment_save_path = _runtime_data / "saved"

cookie_path = _runtime_data / "cookies.json"

for directory in (attachment_save_path,):
    if not directory.exists():
        directory.mkdir()
