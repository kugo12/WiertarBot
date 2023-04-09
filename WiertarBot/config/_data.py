from typing import Protocol, ClassVar, Optional, Self

import wbglobals


class WiertarBotConfig(Protocol):
    instance: ClassVar[Self] = wbglobals.fb_properties

    def getEmail(self) -> str: ...

    def getPassword(self) -> str: ...


class SentryConfig(Protocol):
    instance: ClassVar[Optional[Self]] = wbglobals.sentry_properties

    def getDsn(self) -> str: ...

    def getEnvironment(self) -> str: ...

    def getSampleRate(self) -> float: ...
