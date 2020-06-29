import fbchat

from .. import perm
from ..dispatch import MessageEventDispatcher, Response
from ..config import prefix


@MessageEventDispatcher.register(aliases=['pomoc'])
async def help(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} (komenda)
    Zwraca:
        aktualny prefix i lista komend
        z argumentem informacje o podanej komendzie
    """

    text = event.message.text.lower().replace(prefix, '')
    if text.count(' '):
        arg = text.split(' ', 1)[1]

        if arg in MessageEventDispatcher._alias_of:
            arg = MessageEventDispatcher._alias_of[arg]

        if arg in MessageEventDispatcher._commands:
            msg = MessageEventDispatcher._commands[arg].__doc__
            if not msg:
                msg = 'Podana komenda nie posiada dokumentacji'

        else:
            msg = 'Nie znaleziono podanej komendy'

    else:
        cmd = ', '.join(MessageEventDispatcher._commands)
        msg = (
            f'Prefix: { prefix }\n'
            f'Komendy: { cmd }'
        )

    return Response(event, text=msg)


@MessageEventDispatcher.register()
async def tid(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        id aktualnego wątku
    """

    return Response(event, text=event.thread.id)


@MessageEventDispatcher.register()
async def uid(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} (oznaczenie)
    Zwraca:
        twoje id lub oznaczonej osoby
    """

    if event.message.mentions:
        msg = event.message.mentions[0].thread_id
    else:
        msg = event.author.id

    return Response(event, text=msg)


# TODO: rewrite it in future XD
# TODO: real status instead of always positive
@MessageEventDispatcher.register(name='perm')
async def _perm(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} look <nazwa>
        {command} <add/rem> <nazwa> <wl/bl> (tid=here/tid) <oznaczenia/uid>
    Zwraca:
        status, lub tablice permisji
    """

    msg = _perm.__doc__

    cmd = event.message.text.split(' ')
    if len(cmd) == 3:
        if cmd[1] == 'look':
            perms = perm._get(cmd[2])
            if perms:
                msg = (
                    f'{ cmd[2] }:\n\n'
                    f'whitelist: { perms[0] }\n'
                    f'blacklist: { perms[1] }'
                )
            else:
                msg = 'Podana permisja nie istnieje'

    elif len(cmd) > 4:
        tid = False
        if cmd[4].startswith('tid='):
            try:
                tid = str(int(cmd[4][4:]))
            except ValueError:
                if cmd[4][4:] == 'here':
                    tid = event.thread.id

        # whitelist if true else blacklist
        bl = 0 if cmd[3] == 'wl' else 1

        perms = perm._get(cmd[2])
        add = cmd[1] == 'add'

        # if remove from not existing permissions
        if not add and not cmd:
            return Response(event, text='Podana permisja nie istnieje')

        uids = cmd[3:]
        for mention in event.message.mentions:
            uids.append(mention.thread_id)

        msg = 'Pomyślnie '
        msg += 'dodano do ' if add else 'usunięto z '
        msg += 'blacklisty' if bl else 'whitelisty'

        perm.edit(cmd[2], uids, bl, add, tid)

    return Response(event, text=msg)


@MessageEventDispatcher.register()
async def ban(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} <oznaczenie/uid>
    Zwraca:
        status
    """

    base = prefix+'perm add banned wl '
    without_fw = event.message.text.split(' ', 1)[1]

    event.message.text = base + without_fw
    await _perm(event)

    return Response(event, text='Pomyślnie zbanowano')


@MessageEventDispatcher.register()
async def unban(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} <oznaczenie/uid>
    Zwraca:
        status
    """

    base = prefix+'perm rem banned wl '
    without_fw = event.message.text.split(' ', 1)[1]

    event.message.text = base + without_fw
    await _perm(event)

    return Response(event, text='Pomyślnie odbanowano')
