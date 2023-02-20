from typing import Protocol
import wbglobals


class IPermissionService(Protocol):
    def isAuthorized(self, command: str, thread_id: str, author_id: str) -> bool: ...


class IRabbitMQService(Protocol):
    def publishMessageEvent(self, event: str) -> None: ...

    def publishMessageDelete(self, event: str) -> None: ...

    def publishAccountLocked(self) -> None: ...


PermissionService: IPermissionService = wbglobals.permission_service
RabbitMQService: IRabbitMQService = wbglobals.rabbitmq_service
