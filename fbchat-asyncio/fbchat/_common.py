import sys
import attr
import logging

log = logging.getLogger("fbchat")
req_log = logging.getLogger("fbchat.request")

#: Default attrs settings for classes
attrs_default = attr.s(frozen=True, slots=True, kw_only=True, auto_attribs=True)
