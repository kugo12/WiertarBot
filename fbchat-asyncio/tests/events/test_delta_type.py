import datetime
import pytest
from fbchat import (
    _util,
    ParseError,
    User,
    Group,
    Message,
    MessageData,
    UnknownEvent,
    ColorSet,
    EmojiSet,
    NicknameSet,
    AdminsAdded,
    AdminsRemoved,
    ApprovalModeSet,
    CallStarted,
    CallEnded,
    CallJoined,
)
from fbchat._events import parse_admin_message


def test_color_set(session):
    data = {
        "irisSeqId": "1111111",
        "irisTags": ["DeltaAdminTextMessage", "is_from_iris_fanout"],
        "messageMetadata": {
            "actorFbId": "1234",
            "adminText": "You changed the chat theme to Orange.",
            "folderId": {"systemFolderId": "INBOX"},
            "messageId": "mid.$XYZ",
            "offlineThreadingId": "11223344556677889900",
            "skipBumpThread": False,
            "tags": ["source:titan:web", "no_push"],
            "threadKey": {"threadFbId": "4321"},
            "threadReadStateEffect": "MARK_UNREAD",
            "timestamp": "1500000000000",
            "unsendType": "deny_log_message",
        },
        "participants": ["1234", "2345", "3456"],
        "requestContext": {"apiArgs": {}},
        "tqSeqId": "1111",
        "type": "change_thread_theme",
        "untypedData": {
            "should_show_icon": "1",
            "theme_color": "FFFF7E29",
            "accessibility_label": "Orange",
        },
        "class": "AdminTextMessage",
    }
    assert ColorSet(
        author=User(session=session, id="1234"),
        thread=Group(session=session, id="4321"),
        color="#ff7e29",
        at=datetime.datetime(2017, 7, 14, 2, 40, tzinfo=datetime.timezone.utc),
    ) == parse_admin_message(session, data)


def test_emoji_set(session):
    data = {
        "irisSeqId": "1111111",
        "irisTags": ["DeltaAdminTextMessage"],
        "messageMetadata": {
            "actorFbId": "1234",
            "adminText": "You set the emoji to 🌟.",
            "folderId": {"systemFolderId": "INBOX"},
            "messageId": "mid.$XYZ",
            "offlineThreadingId": "11223344556677889900",
            "skipBumpThread": False,
            "skipSnippetUpdate": False,
            "tags": ["source:generic_admin_text"],
            "threadKey": {"otherUserFbId": "1234"},
            "threadReadStateEffect": "MARK_UNREAD",
            "timestamp": "1500000000000",
            "unsendType": "deny_log_message",
        },
        "requestContext": {"apiArgs": {}},
        "type": "change_thread_icon",
        "untypedData": {
            "thread_icon_url": "https://www.facebook.com/images/emoji.php/v9/te0/1/16/1f31f.png",
            "thread_icon": "🌟",
        },
        "class": "AdminTextMessage",
    }
    assert EmojiSet(
        author=User(session=session, id="1234"),
        thread=User(session=session, id="1234"),
        emoji="🌟",
        at=datetime.datetime(2017, 7, 14, 2, 40, tzinfo=datetime.timezone.utc),
    ) == parse_admin_message(session, data)


def test_nickname_set(session):
    data = {
        "irisSeqId": "1111111",
        "irisTags": ["DeltaAdminTextMessage", "is_from_iris_fanout"],
        "messageMetadata": {
            "actorFbId": "1234",
            "adminText": "You set the nickname for Abc Def to abc.",
            "folderId": {"systemFolderId": "INBOX"},
            "messageId": "mid.$XYZ",
            "offlineThreadingId": "11223344556677889900",
            "skipBumpThread": False,
            "tags": ["source:titan:web", "no_push"],
            "threadKey": {"threadFbId": "4321"},
            "threadReadStateEffect": "MARK_UNREAD",
            "timestamp": "1500000000000",
            "unsendType": "deny_log_message",
        },
        "participants": ["1234", "2345", "3456"],
        "requestContext": {"apiArgs": {}},
        "tqSeqId": "1111",
        "type": "change_thread_nickname",
        "untypedData": {"nickname": "abc", "participant_id": "2345"},
        "class": "AdminTextMessage",
    }
    assert NicknameSet(
        author=User(session=session, id="1234"),
        thread=Group(session=session, id="4321"),
        subject=User(session=session, id="2345"),
        nickname="abc",
        at=datetime.datetime(2017, 7, 14, 2, 40, tzinfo=datetime.timezone.utc),
    ) == parse_admin_message(session, data)


def test_nickname_clear(session):
    data = {
        "irisSeqId": "1111111",
        "irisTags": ["DeltaAdminTextMessage"],
        "messageMetadata": {
            "actorFbId": "1234",
            "adminText": "You cleared your nickname.",
            "folderId": {"systemFolderId": "INBOX"},
            "messageId": "mid.$XYZ",
            "offlineThreadingId": "11223344556677889900",
            "skipBumpThread": False,
            "skipSnippetUpdate": False,
            "tags": ["source:generic_admin_text"],
            "threadKey": {"otherUserFbId": "1234"},
            "threadReadStateEffect": "MARK_UNREAD",
            "timestamp": "1500000000000",
            "unsendType": "deny_log_message",
        },
        "requestContext": {"apiArgs": {}},
        "type": "change_thread_nickname",
        "untypedData": {"nickname": "", "participant_id": "1234"},
        "class": "AdminTextMessage",
    }
    assert NicknameSet(
        author=User(session=session, id="1234"),
        thread=User(session=session, id="1234"),
        subject=User(session=session, id="1234"),
        nickname=None,
        at=datetime.datetime(2017, 7, 14, 2, 40, tzinfo=datetime.timezone.utc),
    ) == parse_admin_message(session, data)


def test_admins_added(session):
    data = {
        "irisSeqId": "1111111",
        "irisTags": ["DeltaAdminTextMessage", "is_from_iris_fanout"],
        "messageMetadata": {
            "actorFbId": "1234",
            "adminText": "You added Abc Def as a group admin.",
            "folderId": {"systemFolderId": "INBOX"},
            "messageId": "mid.$XYZ",
            "offlineThreadingId": "11223344556677889900",
            "skipBumpThread": True,
            "tags": ["source:titan:web"],
            "threadKey": {"threadFbId": "4321"},
            "threadReadStateEffect": "KEEP_AS_IS",
            "timestamp": "1500000000000",
            "unsendType": "deny_log_message",
        },
        "participants": ["1234", "2345", "3456"],
        "requestContext": {"apiArgs": {}},
        "tqSeqId": "1111",
        "type": "change_thread_admins",
        "untypedData": {
            "THREAD_CATEGORY": "GROUP",
            "TARGET_ID": "2345",
            "ADMIN_TYPE": "0",
            "ADMIN_EVENT": "add_admin",
        },
        "class": "AdminTextMessage",
    }
    assert AdminsAdded(
        author=User(session=session, id="1234"),
        thread=Group(session=session, id="4321"),
        added=[User(session=session, id="2345")],
        at=datetime.datetime(2017, 7, 14, 2, 40, tzinfo=datetime.timezone.utc),
    ) == parse_admin_message(session, data)


def test_admins_removed(session):
    data = {
        "irisSeqId": "1111111",
        "irisTags": ["DeltaAdminTextMessage", "is_from_iris_fanout"],
        "messageMetadata": {
            "actorFbId": "1234",
            "adminText": "You removed yourself as a group admin.",
            "folderId": {"systemFolderId": "INBOX"},
            "messageId": "mid.$XYZ",
            "offlineThreadingId": "11223344556677889900",
            "skipBumpThread": True,
            "tags": ["source:titan:web"],
            "threadKey": {"threadFbId": "4321"},
            "threadReadStateEffect": "KEEP_AS_IS",
            "timestamp": "1500000000000",
            "unsendType": "deny_log_message",
        },
        "participants": ["1234", "2345", "3456"],
        "requestContext": {"apiArgs": {}},
        "tqSeqId": "1111",
        "type": "change_thread_admins",
        "untypedData": {
            "THREAD_CATEGORY": "GROUP",
            "TARGET_ID": "1234",
            "ADMIN_TYPE": "0",
            "ADMIN_EVENT": "remove_admin",
        },
        "class": "AdminTextMessage",
    }
    assert AdminsRemoved(
        author=User(session=session, id="1234"),
        thread=Group(session=session, id="4321"),
        removed=[User(session=session, id="1234")],
        at=datetime.datetime(2017, 7, 14, 2, 40, tzinfo=datetime.timezone.utc),
    ) == parse_admin_message(session, data)


def test_approvalmode_set(session):
    data = {
        "irisSeqId": "1111111",
        "irisTags": ["DeltaAdminTextMessage", "is_from_iris_fanout"],
        "messageMetadata": {
            "actorFbId": "1234",
            "adminText": "You turned on member approval and will review requests to join the group.",
            "folderId": {"systemFolderId": "INBOX"},
            "messageId": "mid.$XYZ",
            "offlineThreadingId": "11223344556677889900",
            "skipBumpThread": True,
            "tags": ["source:titan:web", "no_push"],
            "threadKey": {"threadFbId": "4321"},
            "threadReadStateEffect": "KEEP_AS_IS",
            "timestamp": "1500000000000",
            "unsendType": "deny_log_message",
        },
        "participants": ["1234", "2345", "3456"],
        "requestContext": {"apiArgs": {}},
        "tqSeqId": "1111",
        "type": "change_thread_approval_mode",
        "untypedData": {"APPROVAL_MODE": "1", "THREAD_CATEGORY": "GROUP"},
        "class": "AdminTextMessage",
    }
    assert ApprovalModeSet(
        author=User(session=session, id="1234"),
        thread=Group(session=session, id="4321"),
        require_admin_approval=True,
        at=datetime.datetime(2017, 7, 14, 2, 40, tzinfo=datetime.timezone.utc),
    ) == parse_admin_message(session, data)


def test_approvalmode_unset(session):
    data = {
        "irisSeqId": "1111111",
        "irisTags": ["DeltaAdminTextMessage", "is_from_iris_fanout"],
        "messageMetadata": {
            "actorFbId": "1234",
            "adminText": "You turned off member approval. Anyone with the link can join the group.",
            "folderId": {"systemFolderId": "INBOX"},
            "messageId": "mid.$XYZ",
            "offlineThreadingId": "11223344556677889900",
            "skipBumpThread": True,
            "tags": ["source:titan:web", "no_push"],
            "threadKey": {"threadFbId": "4321"},
            "threadReadStateEffect": "KEEP_AS_IS",
            "timestamp": "1500000000000",
            "unsendType": "deny_log_message",
        },
        "participants": ["1234", "2345", "3456"],
        "requestContext": {"apiArgs": {}},
        "tqSeqId": "1111",
        "type": "change_thread_approval_mode",
        "untypedData": {"APPROVAL_MODE": "0", "THREAD_CATEGORY": "GROUP"},
        "class": "AdminTextMessage",
    }
    assert ApprovalModeSet(
        author=User(session=session, id="1234"),
        thread=Group(session=session, id="4321"),
        require_admin_approval=False,
        at=datetime.datetime(2017, 7, 14, 2, 40, tzinfo=datetime.timezone.utc),
    ) == parse_admin_message(session, data)


def test_call_started(session):
    data = {
        "irisSeqId": "1111111",
        "irisTags": ["DeltaAdminTextMessage", "is_from_iris_fanout"],
        "messageMetadata": {
            "actorFbId": "1234",
            "adminText": "You started a call.",
            "folderId": {"systemFolderId": "INBOX"},
            "messageId": "mid.$XYZ",
            "offlineThreadingId": "11223344556677889900",
            "skipBumpThread": False,
            "tags": ["source:titan:web", "no_push"],
            "threadKey": {"threadFbId": "4321"},
            "threadReadStateEffect": "MARK_UNREAD",
            "timestamp": "1500000000000",
            "unsendType": "deny_log_message",
        },
        "participants": ["1234", "2345", "3456"],
        "requestContext": {"apiArgs": {}},
        "tqSeqId": "1111",
        "type": "messenger_call_log",
        "untypedData": {
            "call_capture_attachments": "",
            "caller_id": "1234",
            "conference_name": "MESSENGER:134845267536444",
            "rating": "",
            "messenger_call_instance_id": "0",
            "video": "",
            "event": "group_call_started",
            "server_info": "XYZ123ABC",
            "call_duration": "0",
            "callee_id": "0",
        },
        "class": "AdminTextMessage",
    }
    data2 = {
        "callState": "AUDIO_GROUP_CALL",
        "messageMetadata": {
            "actorFbId": "1234",
            "folderId": {"systemFolderId": "INBOX"},
            "messageId": "mid.$XYZ",
            "offlineThreadingId": "11223344556677889900",
            "skipBumpThread": False,
            "tags": [],
            "threadKey": {"threadFbId": "4321"},
            "threadReadStateEffect": "KEEP_AS_IS",
            "timestamp": "1500000000000",
        },
        "serverInfoData": "XYZ123ABC",
        "class": "RtcCallData",
    }
    assert CallStarted(
        author=User(session=session, id="1234"),
        thread=Group(session=session, id="4321"),
        at=datetime.datetime(2017, 7, 14, 2, 40, tzinfo=datetime.timezone.utc),
    ) == parse_admin_message(session, data)


def test_group_call_ended(session):
    data = {
        "irisSeqId": "1111111",
        "irisTags": ["DeltaAdminTextMessage", "is_from_iris_fanout"],
        "messageMetadata": {
            "actorFbId": "1234",
            "adminText": "The call ended.",
            "folderId": {"systemFolderId": "INBOX"},
            "messageId": "mid.$XYZ",
            "offlineThreadingId": "11223344556677889900",
            "skipBumpThread": False,
            "tags": ["source:titan:web", "no_push"],
            "threadKey": {"threadFbId": "4321"},
            "threadReadStateEffect": "MARK_UNREAD",
            "timestamp": "1500000000000",
            "unsendType": "deny_log_message",
        },
        "participants": ["1234", "2345", "3456"],
        "requestContext": {"apiArgs": {}},
        "tqSeqId": "1111",
        "type": "messenger_call_log",
        "untypedData": {
            "call_capture_attachments": "",
            "caller_id": "1234",
            "conference_name": "MESSENGER:1234567890",
            "rating": "0",
            "messenger_call_instance_id": "1234567890",
            "video": "",
            "event": "group_call_ended",
            "server_info": "XYZ123ABC",
            "call_duration": "31",
            "callee_id": "0",
        },
        "class": "AdminTextMessage",
    }
    data2 = {
        "callState": "NO_ONGOING_CALL",
        "messageMetadata": {
            "actorFbId": "1234",
            "folderId": {"systemFolderId": "INBOX"},
            "messageId": "mid.$XYZ",
            "offlineThreadingId": "11223344556677889900",
            "skipBumpThread": False,
            "tags": [],
            "threadKey": {"threadFbId": "4321"},
            "threadReadStateEffect": "KEEP_AS_IS",
            "timestamp": "1500000000000",
        },
        "class": "RtcCallData",
    }
    assert CallEnded(
        author=User(session=session, id="1234"),
        thread=Group(session=session, id="4321"),
        duration=datetime.timedelta(seconds=31),
        at=datetime.datetime(2017, 7, 14, 2, 40, tzinfo=datetime.timezone.utc),
    ) == parse_admin_message(session, data)


def test_user_call_ended(session):
    data = {
        "irisSeqId": "1111111",
        "irisTags": ["DeltaAdminTextMessage", "is_from_iris_fanout"],
        "messageMetadata": {
            "actorFbId": "1234",
            "adminText": "Abc called you.",
            "folderId": {"systemFolderId": "INBOX"},
            "messageId": "mid.$XYZ",
            "offlineThreadingId": "11223344556677889900",
            "skipBumpThread": False,
            "skipSnippetUpdate": False,
            "tags": ["source:generic_admin_text", "no_push"],
            "threadKey": {"otherUserFbId": "1234"},
            "threadReadStateEffect": "KEEP_AS_IS",
            "timestamp": "1500000000000",
            "unsendType": "deny_log_message",
        },
        "requestContext": {"apiArgs": {}},
        "type": "messenger_call_log",
        "untypedData": {
            "call_capture_attachments": "",
            "caller_id": "1234",
            "conference_name": "MESSENGER:1234567890",
            "rating": "0",
            "messenger_call_instance_id": "1234567890",
            "video": "",
            "event": "one_on_one_call_ended",
            "server_info": "",
            "call_duration": "3",
            "callee_id": "100002950119740",
        },
        "class": "AdminTextMessage",
    }
    assert CallEnded(
        author=User(session=session, id="1234"),
        thread=User(session=session, id="1234"),
        duration=datetime.timedelta(seconds=3),
        at=datetime.datetime(2017, 7, 14, 2, 40, tzinfo=datetime.timezone.utc),
    ) == parse_admin_message(session, data)


def test_call_joined(session):
    data = {
        "irisSeqId": "1111111",
        "irisTags": ["DeltaAdminTextMessage", "is_from_iris_fanout"],
        "messageMetadata": {
            "actorFbId": "1234",
            "adminText": "Abc joined the call.",
            "folderId": {"systemFolderId": "INBOX"},
            "messageId": "mid.$XYZ",
            "offlineThreadingId": "11223344556677889900",
            "skipBumpThread": False,
            "tags": ["source:titan:web"],
            "threadKey": {"threadFbId": "4321"},
            "threadReadStateEffect": "MARK_UNREAD",
            "timestamp": "1500000000000",
            "unsendType": "deny_log_message",
        },
        "participants": ["1234", "2345", "3456"],
        "requestContext": {"apiArgs": {}},
        "tqSeqId": "1111",
        "type": "participant_joined_group_call",
        "untypedData": {
            "server_info_data": "XYZ123ABC",
            "group_call_type": "0",
            "joining_user": "2345",
        },
        "class": "AdminTextMessage",
    }
    assert CallJoined(
        author=User(session=session, id="1234"),
        thread=Group(session=session, id="4321"),
        at=datetime.datetime(2017, 7, 14, 2, 40, tzinfo=datetime.timezone.utc),
    ) == parse_admin_message(session, data)


def test_parse_admin_message_unknown(session):
    data = {"class": "AdminTextMessage", "type": "abc"}
    assert UnknownEvent(source="Delta type", data=data) == parse_admin_message(
        session, data
    )
