from typing import Protocol, Any, Optional

from WiertarBot.database import FBMessage, MessageCountMilestone
import wbglobals


class IFBMessageRepository(Protocol):
    def saveAndFlush(self, message: FBMessage) -> FBMessage: ...
    def findAllByDeletedAtNullAndTimeBefore(self, time: int) -> list[FBMessage]: ...

    def findAllByThreadIdAndDeletedAtNotNull(self, threadId: str, pageable: Any) -> list[FBMessage]: ...

    def markDeleted(self, messageId: str, timestamp: int) -> None: ...

    def deleteByDeletedAtNullAndTimeBefore(self, time: int) -> None: ...


class IMessageCountMilestoneRepository(Protocol):
    def saveAndFlush(self, milestone: MessageCountMilestone) -> MessageCountMilestone: ...
    def findFirstByThreadId(self, threadId: str) -> Optional[MessageCountMilestone]: ...


FBMessageRepository: IFBMessageRepository = wbglobals.fb_message_repository
MessageCountMilestoneRepository: IMessageCountMilestoneRepository = wbglobals.milestone_repository
