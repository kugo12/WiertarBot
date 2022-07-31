from typing import Optional

from ._constants import *
from ._data import *

wiertarbot: WiertarBotConfig = config[WiertarBotConfig]
database: DatabaseConfig = config[DatabaseConfig]
sentry: Optional[SentryConfig] = config[SentryConfig]
wiertarbot_stats: Optional[WiertarBotStatsConfig] = config[WiertarBotStatsConfig]
cat_api: Optional[CatApiConfig] = config[CatApiConfig]


try:
    from .unlock import unlock_facebook_account
except ModuleNotFoundError:
    def unlock_facebook_account():
        raise NotImplementedError()
