import json
from typing import List, Optional

from .database import Permission


def get_permission(command: str) -> Optional[Permission]:
    try:
        return Permission.get(Permission.command == command)
    except Permission.DoesNotExist:
        return None


def check(name: str, thread_id: str, user_id: str) -> bool:
    permission = get_permission(name)
    if permission is None:
        return False

    whitelist = json.loads(permission.whitelist)
    blacklist = json.loads(permission.blacklist)

    if "*" in blacklist:
        if user_id != thread_id:
            if thread_id in whitelist:
                if user_id in whitelist[thread_id]:
                    return True
                if "*" in whitelist[thread_id]:
                    if user_id in blacklist:
                        return False
                    if thread_id in blacklist:
                        if user_id in blacklist[thread_id]:
                            return False
                    return True
        if user_id in whitelist:
            if user_id != thread_id:
                if thread_id in blacklist:
                    if user_id in blacklist[thread_id]:
                        return False
                    if "*" in blacklist[thread_id]:
                        return False
            return True
        return False

    if "*" in whitelist:
        if user_id != thread_id:
            if thread_id in blacklist:
                if "*" in blacklist[thread_id]:
                    if user_id in whitelist:
                        return True
                    if thread_id in whitelist:
                        if user_id in whitelist[thread_id]:
                            return True
                    return False
                if user_id in blacklist[thread_id]:
                    return False
        if user_id in blacklist:
            if user_id != thread_id:
                if thread_id in whitelist:
                    if user_id in whitelist[thread_id]:
                        return True
                    if "*" in whitelist[thread_id]:
                        return True
            return False
        return True

    if user_id != thread_id:
        if thread_id in whitelist:
            if "*" in whitelist[thread_id]:
                if thread_id in blacklist:
                    if user_id in blacklist[thread_id]:
                        return False
                return True
            if user_id in whitelist[thread_id]:
                return True

    if user_id in whitelist:
        if whitelist[user_id] == 0:  # {'uid': {'uid': 0}} bug fix
            return True

    return False


def edit(command: str, uids: List[str], bl=False, add=True, tid=False) -> bool:
    permission = get_permission(command)
    blacklist = {}
    whitelist = {}

    if permission is None and not add:
        return False
    elif permission is None:
        permission = Permission(command=command)
    else:
        blacklist = json.loads(permission.blacklist)
        whitelist = json.loads(permission.whitelist)

    currently_edited = blacklist if bl else whitelist

    for uid in uids:
        try:
            int(uid)
        except ValueError:
            if uid != "*":
                continue

        if add:
            if tid:
                if tid not in currently_edited:
                    currently_edited[tid] = []
                currently_edited[tid].append(uid)
            else:
                currently_edited[uid] = 0
        else:
            if tid and tid in currently_edited:
                while uid in currently_edited[tid]:
                    currently_edited[tid].remove(uid)
                if not currently_edited[tid]:
                    currently_edited.pop(tid)
            else:
                currently_edited.pop(uid)

    permission.whitelist = json.dumps(whitelist)
    permission.blacklist = json.dumps(blacklist)
    permission.save()

    return True
