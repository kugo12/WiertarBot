from typing import Optional, Union, TYPE_CHECKING
from pl.kvgx12.wiertarbot.events import Response as KtResponse

if TYPE_CHECKING:
    from .events import MessageEvent, Mention


class IResponse:

    def getEvent(self) -> 'MessageEvent': ...
    def getText(self) -> Optional[str]: ...
    def getFiles(self) -> list[str]: ...
    def getVoiceClip(self) -> bool: ...
    def getMentions(self) -> list['Mention']: ...
    def getReplyToId(self) -> Optional[str]: ...

    async def pySend(self) -> None: ...


def response(
        event: 'MessageEvent',
        text: str | None = None,
        files: list[str] | None = None,
        voice_clip: bool = False,
        mentions: list['Mention'] | None = None,
        reply_to_id: str | None = None
) -> 'IResponse':
    return KtResponse(event, text, files, voice_clip, mentions, reply_to_id)