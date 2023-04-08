import fbchat

from pl.kvgx12.wiertarbot.events import Attachment as KtAttachment, ImageAttachment as KtImageAttachment

from WiertarBot.events import Attachment


def fb_attachment_to_generic(attachment: fbchat.Attachment) -> Attachment:
    if isinstance(attachment, fbchat.ImageAttachment):
        return KtImageAttachment(
            id=attachment.id,
            width=attachment.width,
            height=attachment.height,
            original_extension=attachment.original_extension,
            is_animated=attachment.is_animated
        )
    return KtAttachment(attachment.id)
