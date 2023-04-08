from typing import Protocol, Final

import wbglobals


class WiertarBotConfig(Protocol):
    instance: Final['WiertarBotConfig'] = wbglobals.fb_properties

    def getEmail(self) -> str: ...

    def getPassword(self) -> str: ...


class SentryConfig(Protocol):
    instance: Final['SentryConfig'] = wbglobals.sentry_properties

    def getDsn(self) -> str: ...

    def getEnvironment(self) -> str: ...

    def getSampleRate(self) -> float: ...
