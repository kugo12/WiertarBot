from typing import NoReturn
from abc import ABC, abstractmethod


class Connector(ABC):
    @classmethod
    @abstractmethod
    async def create(cls) -> 'Connector': ...

    @abstractmethod
    async def run(self) -> NoReturn: ...
