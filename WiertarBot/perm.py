import json

from .db import db


def _get(name: str):
    cur = db.get().cursor()
    cur.execute("SELECT * FROM permissions WHERE command = ?", [name])
    a = cur.fetchone()
    if a:
        return [json.loads(a[1]), json.loads(a[2])]
    else:
        return False


def _update_db(name: str, perms) -> bool:
    conn = db.get()
    cur = conn.cursor()
    if _get(name) is False:
        cur.execute("INSERT INTO permissions (command, whitelist, blacklist) VALUES (?, ?, ?)",
                    [name, perms[0], perms[1]])
        conn.commit()
        return True

    cur.execute("UPDATE permissions SET whitelist = ?, blacklist = ? WHERE command = ?",
                [perms[0], perms[1], name])
    conn.commit()
    return False


def check(name: str, thread_id: str, user_id: str) -> bool:
    a = await _get(name)
    # a[0] - whitelist
    # a[1] - blacklist
    if a:
        if "*" in a[1]:
            if user_id != thread_id:
                if thread_id in a[0]:
                    if user_id in a[0][thread_id]:
                        return True
                    if "*" in a[0][thread_id]:
                        if user_id in a[1]:
                            return False
                        if thread_id in a[1]:
                            if user_id in a[1][thread_id]:
                                return False
                        return True
            if user_id in a[0]:
                if user_id != thread_id:
                    if thread_id in a[1]:
                        if user_id in a[1][thread_id]:
                            return False
                        if "*" in a[1][thread_id]:
                            return False
                return True
            return False

        if "*" in a[0]:
            if user_id != thread_id:
                if thread_id in a[1]:
                    if "*" in a[1][thread_id]:
                        if user_id in a[0]:
                            return True
                        if thread_id in a[0]:
                            if user_id in a[0][thread_id]:
                                return True
                        return False
                    if user_id in a[1][thread_id]:
                        return False
            if user_id in a[1]:
                if user_id != thread_id:
                    if thread_id in a[0]:
                        if user_id in a[0][thread_id]:
                            return True
                        if "*" in a[0][thread_id]:
                            return True
                return False
            return True

        if user_id != thread_id:
            if thread_id in a[0]:
                if "*" in a[0][thread_id]:
                    if thread_id in a[1]:
                        if user_id in a[1][thread_id]:
                            return False
                    return True
                if user_id in a[0][thread_id]:
                    return True

        if user_id in a[0]:
            if a[0][user_id] == 0:  # {'uid': {'uid': 0}} bug fix
                return True
    return False


def edit(name: str, uids, bl=False, add=True, tid=False) -> bool:
    cmd = _get(name)
    if (cmd) or ((not cmd) and add):
        bl = 1 if bl else 0
        if not cmd:
            cmd = [{}, {}]

        for uid in uids:
            try:
                int(uid)
            except ValueError:
                if uid != "*":
                    continue
            if tid:
                if add:
                    if (tid in cmd[bl]) is False:
                        cmd[bl][tid] = []
                    cmd[bl][tid].append(uid)
                else:
                    if tid in cmd[bl]:
                        while uid in cmd[bl][tid]:
                            cmd[bl][tid].remove(uid)
                        if cmd[bl][tid] == []:
                            cmd[bl].pop(tid)
            else:
                if add:
                    cmd[bl][uid] = 0
                else:
                    while uid in cmd[bl]:
                        cmd[bl].pop(uid)

        a = [json.dumps(cmd[0]), json.dumps(cmd[1])]
        _update_db(name, a)

        return True
    return False
