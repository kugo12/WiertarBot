from typing import Optional, Final

from ._constants import *
from ._data import *

wiertarbot: Final = config.get(WiertarBotConfig)
database: Final = config.get(DatabaseConfig)
sentry: Final = config[SentryConfig]
wiertarbot_stats: Final = config[WiertarBotStatsConfig]
cat_api: Final = config[CatApiConfig]

try:
    from .unlock import unlock_facebook_account
except ModuleNotFoundError:
    def unlock_facebook_account() -> None:
        raise NotImplementedError()
