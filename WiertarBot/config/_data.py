from ._config import Config
from ._constants import config_path

config = Config(config_path)


@config.properties("wiertarbot")
class WiertarBotConfig:
    email: str
    password: str
    prefix: str = "!"


@config.properties("database")
class DatabaseConfig:
    name: str
    user: str
    password: str
    host: str
    port: int = 5432


@config.properties("sentry", optional=True)
class SentryConfig:
    dsn: str
    environment: str = "dev"
    sample_rate: float = 1.0


@config.properties("wiertarbot.stats_api", optional=True)
class WiertarBotStatsConfig:
    key: str
    message_url: str


@config.properties("wiertarbot.cat_api", optional=True)
class CatApiConfig:
    key: str
