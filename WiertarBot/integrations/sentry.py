from ..config import sentry

if sentry:
    import sentry_sdk

    def init():
        sentry_sdk.init(
            dsn=sentry.dsn,
            traces_sample_rate=sentry.sample_rate,
            environment=sentry.environment
        )


    capture_exception = sentry_sdk.capture_exception
    capture_message = sentry_sdk.capture_message

else:
    def init():
        pass


    def capture_exception(*args, **kwargs):
        pass


    def capture_message(*args, **kwargs):
        pass
