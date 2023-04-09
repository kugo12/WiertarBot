from ... import fbchat
from ...events import Attachment, ImageAttachment


def fb_attachment_to_generic(attachment: fbchat.Attachment) -> Attachment:
    if isinstance(attachment, fbchat.ImageAttachment):
        return ImageAttachment(
            id=attachment.id,
            width=attachment.width,
            height=attachment.height,
            original_extension=attachment.original_extension,
            is_animated=attachment.is_animated
        )
    return Attachment(attachment.id)
