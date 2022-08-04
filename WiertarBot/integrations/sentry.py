from types import TracebackType
from typing import Optional, Any, Union

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
    _ExceptionInfo = tuple[
        Optional[type[BaseException]], Optional[BaseException], Optional[TracebackType]
    ]


    def init():
        pass


    def capture_exception(
            error: Optional[Union[BaseException, _ExceptionInfo]] = None,
            scope: Optional[Any] = None,
            **scope_args: Any
    ) -> Optional[str]:
        pass


    def capture_message(
            message: str,
            level: Optional[str] = None,
            scope: Optional[Any] = None,
            **scope_args: Any
    ) -> Optional[str]:
        return None
