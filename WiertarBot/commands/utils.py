import json
import asyncio
from typing import List, Awaitable, Optional

from ..config import config
# fixme
# from .. import perm, config
from ..message_dispatch import MessageEventDispatcher
from ..events import MessageEvent, Mention
from ..response import IResponse, response
from ..database import PermissionRepository, FBMessageRepository


@MessageEventDispatcher.register(aliases=['pomoc'])
async def help(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command} (komenda)
    Zwraca:
        aktualny prefix i lista komend
        z argumentem informacje o podanej komendzie
    """

    text = event.getText().lower().replace(config.wiertarbot.prefix, '')
    if text.count(' '):
        arg = text.split(' ', 1)[1]

        command = MessageEventDispatcher.command(arg)
        if command:
            msg = command.__doc__
            if not msg:
                msg = 'Podana komenda nie posiada dokumentacji'
        else:
            msg = 'Nie znaleziono podanej komendy'

    else:
        cmd = ', '.join(MessageEventDispatcher.commands())
        msg = (
            f'Prefix: { config.wiertarbot.prefix }\n'
            f'Komendy: { cmd }'
        )

    return response(event, text=msg)


@MessageEventDispatcher.register()
async def tid(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command}
    Zwraca:
        id aktualnego wątku
    """

    return response(event, text=event.getThreadId())


@MessageEventDispatcher.register()
async def uid(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command} (oznaczenie)
    Zwraca:
        twoje id lub oznaczonej osoby
    """

    if event.getMentions():
        msg = event.getMentions()[0].getThreadId()
    else:
        msg = event.getAuthorId()

    return response(event, text=msg)


# TODO: rewrite it in future XD
# TODO: real status instead of always positive
@MessageEventDispatcher.register(name='perm')
async def _perm(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command} look <nazwa>
        {command} <add/rem> <nazwa> <wl/bl> (tid=here/tid) <oznaczenia/uid>
    Zwraca:
        status, lub tablice permisji
    """

    msg = _perm.__doc__

    cmd = event.getText().split(' ')
    if len(cmd) == 3:
        if cmd[1] == 'look':
            perms = PermissionRepository.findFirstByCommand(cmd[2])
            if perms:
                msg = (
                    f'{ cmd[2] }:\n\n'
                    f'whitelist: { perms.getWhitelist() }\n'
                    f'blacklist: { perms.getBlacklist() }'
                )
            else:
                msg = 'Podana permisja nie istnieje'

    elif len(cmd) > 4:
        tid = None
        if cmd[4].startswith('tid='):
            try:
                tid = str(int(cmd[4][4:]))
            except ValueError:
                if cmd[4][4:] == 'here':
                    tid = event.getThreadId()

        bl = cmd[3] != 'wl'
        add = cmd[1] == 'add'

        # if remove from not existing permissions
        if not add and not cmd:
            return response(event, text='Podana permisja nie istnieje')

        uids = cmd[3:]
        for mention in event.getMentions():
            uids.append(mention.getThreadId())

        msg = 'Pomyślnie '
        msg += 'dodano do ' if add else 'usunięto z '
        msg += 'blacklisty' if bl else 'whitelisty'

        perm.edit(cmd[2], uids, bl, add, tid)

    return response(event, text=msg)


@MessageEventDispatcher.register()
async def ban(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command} <oznaczenie/uid>
    Zwraca:
        status
    """

    base = config.wiertarbot.prefix+'perm add banned wl '
    without_fw = event.getText().split(' ', 1)[1]

    # fixme
    await _perm(
        event.copyWithDifferentText(base + without_fw)
    )

    return response(event, text='Pomyślnie zbanowano')


@MessageEventDispatcher.register()
async def unban(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command} <oznaczenie/uid>
    Zwraca:
        status
    """

    base = config.wiertarbot.prefix+'perm rem banned wl '
    without_fw = event.getText().split(' ', 1)[1]

    await _perm(
        event.copyWithDifferentText(base + without_fw)
    )

    return response(event, text='Pomyślnie odbanowano')


@MessageEventDispatcher.register()
async def ile(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command}
    Zwraca:
        ilość napisanych wiadomości od dodania bota
    """

    thread = await event.getContext().fetch_thread(event.getThreadId())

    msg = f'Odkąd tutaj jestem napisano tu { thread.message_count } wiadomości.'

    return response(event, text=msg)


@MessageEventDispatcher.register()
async def uptime(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command}
    Zwraca:
        pokazuje czas od uruchomienia serwera
    """

    with open('/proc/uptime', 'r') as f:
        up = int(float(f.readline().split()[0]))

    d = up//86400
    h = up//3600 % 24
    m = up//60 % 60

    msg = f'Serwer jest uruchomiony od { d }d { h }h { m }m'

    return response(event, text=msg)


@MessageEventDispatcher.register()
async def see(event: MessageEvent) -> Optional[IResponse]:
    """
    Użycie:
        {command} (ilosc<=10)
    Zwraca:
        jedną lub więcej ostatnio usuniętych wiadomości w wątku
    """

    try:
        n = int(event.getText().split(' ', 1)[1])
        if n > 10:
            n = 10
        elif n < 1:
            n = 1
    except (IndexError, ValueError):
        n = 1

    messages = FBMessageRepository.find_deleted_by_thread_id(event.thread_id, n)

    send_responses: List[Awaitable] = []
    for it in messages:
        message = json.loads(it.message)

        mentions = [
            Mention(**mention)
            for mention in message['mentions']
        ]

        voice_clip = False
        files = []
        for att in message['attachments']:
            if att['type'] == 'ImageAttachment':
                p = config.attachment_save_path / f'{ att["id"] }.{ att["original_extension"] }'
                files.append(str(p))
            elif att['type'] == 'AudioAttachment':
                p = config.attachment_save_path / att['filename']
                files.append(str(p))
                voice_clip = True
            elif att['type'] == 'VideoAttachment':
                p = config.attachment_save_path / f'{ att["id"] }.mp4'
                files.append(str(p))

        r = response(
            event,
            text=message['text'],
            mentions=mentions,
            files=files,
            voice_clip=voice_clip
        )
        send_responses.append(r.pySend())

    if send_responses:
        await asyncio.gather(*send_responses)
        return None
    else:
        return response(event, text='Nie ma żadnych zapisanych usuniętych wiadomości w tym wątku')
