import attr
from . import Image
from .. import _util

from typing import Optional, Sequence, Self


@attr.s(frozen=True, slots=True, kw_only=True, auto_attribs=True)
class Attachment:
    """Represents a Facebook attachment."""

    #: The attachment ID
    id: Optional[str] = None


@attr.s(frozen=True, slots=True, kw_only=True, auto_attribs=True)
class UnsentMessage(Attachment):
    """Represents an unsent message attachment."""


@attr.s(frozen=True, slots=True, kw_only=True, auto_attribs=True)
class ShareAttachment(Attachment):
    """Represents a shared item (e.g. URL) attachment."""

    #: ID of the author of the shared post
    author: Optional[str] = None
    #: Target URL
    url: Optional[str] = None
    #: Original URL if Facebook redirects the URL
    original_url: Optional[str] = None
    #: Title of the attachment
    title: Optional[str] = None
    #: Description of the attachment
    description: Optional[str] = None
    #: Name of the source
    source: Optional[str] = None
    #: The attached image
    image: Optional[Image] = None
    #: URL of the original image if Facebook uses ``safe_image``
    original_image_url: Optional[str] = None
    #: List of additional attachments
    attachments: Sequence[Attachment] = attr.ib(factory=list)

    @classmethod
    def _from_graphql(cls, data) -> Self:
        from . import _file

        image = None
        original_image_url = None
        media = data.get("media")
        if media and media.get("image"):
            image = Image._from_uri(media["image"])
            original_image_url = (
                _util.get_url_parameter(image.url, "url")
                if "/safe_image.php" in image.url
                else image.url
            )

        url = data.get("url")
        attachments = [
            _file.graphql_to_subattachment(attachment)
            for attachment in data.get("subattachments")
        ]
        return cls(
            id=data.get("deduplication_key"),
            author=data["target"]["actors"][0]["id"]
            if data["target"].get("actors")
            else None,
            url=url,
            original_url=_util.get_url_parameter(url, "u")
            if "/l.php?u=" in url
            else url,
            title=data["title_with_entities"].get("text"),
            description=data["description"].get("text")
            if data.get("description")
            else None,
            source=data["source"].get("text") if data.get("source") else None,
            image=image,
            original_image_url=original_image_url,
            attachments=[it for it in attachments if it],
        )