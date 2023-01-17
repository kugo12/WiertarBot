import pytz
from functools import cached_property

from ._config import Config
from ._constants import config_path

config = Config(config_path)


@config.properties("wiertarbot")
class WiertarBotConfig:
    email: str
    password: str
    prefix: str = "!"
    timezone: str = "Europe/Warsaw"

    @cached_property
    def tz(self) -> pytz.BaseTzInfo:
        return pytz.timezone(self.timezone)


@config.properties("database")
class DatabaseConfig:
    name: str
    user: str
    password: str
    host: str
    port: int = 5432

    @property
    def url(self) -> str:
        return f"postgresql+psycopg2://{self.user}:{self.password}@{self.host}:{self.port}/{self.name}"


@config.properties("sentry", optional=True)
class SentryConfig:
    dsn: str
    environment: str = "dev"
    sample_rate: float = 1.0


@config.properties("wiertarbot.cat_api", optional=True)
class CatApiConfig:
    key: str


@config.properties("rabbitmq", optional=True)
class RabbitMQConfig:
    url: str
    exchange_name: str = "bot.default"


@config.properties("health", optional=True)
class HealthConfig:
    port: int
    host: str = "0.0.0.0"
    access_log: bool = False
