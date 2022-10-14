import sentry_sdk

from ..config import config, SentryConfig

_on = config.on(SentryConfig)

@config.register_init
@_on
def init(sentry: SentryConfig) -> None:
    sentry_sdk.init(
        dsn=sentry.dsn,
        traces_sample_rate=sentry.sample_rate,
        environment=sentry.environment
    )


capture_exception = _on(sentry_sdk.capture_exception)
capture_message = _on(sentry_sdk.capture_message)
