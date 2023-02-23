import pytz
from functools import cached_property

from ._config import Config
import wbglobals

config = Config(wbglobals.config)


@config.properties("wiertarbot")
class WiertarBotConfig:
    email: str
    password: str
    prefix: str = "!"
    timezone: str = "Europe/Warsaw"

    @cached_property
    def tz(self) -> pytz.BaseTzInfo:
        return pytz.timezone(self.timezone)


@config.properties("wiertarbot.sentry.python", optional=True)
class SentryConfig:
    dsn: str
    environment: str = "dev"
    sample_rate: float = 1.0


@config.properties("wiertarbot.catApi", optional=True)
class CatApiConfig:
    key: str


@config.properties("rabbitmq", optional=True)
class RabbitMQConfig:
    url: str
    exchange_name: str = "bot.default"
