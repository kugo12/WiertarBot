from typing import Final

from ._constants import *
from ._data import *

wiertarbot: Final = config[WiertarBotConfig]
sentry: Final = config.get(SentryConfig)
rabbitmq: Final = config.get(RabbitMQConfig)
