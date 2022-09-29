from typing import Final

from ._constants import *
from ._data import *

wiertarbot: Final = config[WiertarBotConfig]
database: Final = config[DatabaseConfig]
sentry: Final = config.get(SentryConfig)
wiertarbot_stats: Final = config.get(WiertarBotStatsConfig)
cat_api: Final = config.get(CatApiConfig)
