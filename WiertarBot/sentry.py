import sentry_sdk

from .config import sentry

if sentry:
    sentry_sdk.init(
        dsn=sentry.getDsn(),
        traces_sample_rate=sentry.getSampleRate(),
        environment=sentry.getEnvironment()
    )
else:
    sentry_sdk.init(sample_rate=1.0)
