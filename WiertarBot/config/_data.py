from ._config import Config
import wbglobals

config = Config(wbglobals.config)


@config.properties("wiertarbot.fb")
class WiertarBotConfig:
    email: str
    password: str


@config.properties("wiertarbot.sentry.python", optional=True)
class SentryConfig:
    dsn: str
    environment: str = "dev"
    sample_rate: float = 1.0
