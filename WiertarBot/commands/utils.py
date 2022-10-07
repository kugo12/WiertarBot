import aiohttp
import fbchat
import json
import asyncio
from io import BytesIO
from typing import List, Awaitable, Optional

from .. import perm, config
from ..dispatch import MessageEventDispatcher
from ..events import MessageEvent, Mention
from ..response import Response
from ..bot import WiertarBot
from ..database import PermissionRepository, FBMessageRepository


@MessageEventDispatcher.register(aliases=['pomoc'])
async def help(event: MessageEvent) -> Response:
    """
    Użycie:
        {command} (komenda)
    Zwraca:
        aktualny prefix i lista komend
        z argumentem informacje o podanej komendzie
    """

    text = event.text.lower().replace(config.wiertarbot.prefix, '')
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

    return Response(event, text=msg)


@MessageEventDispatcher.register()
async def tid(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        id aktualnego wątku
    """

    return Response(event, text=event.thread_id)


@MessageEventDispatcher.register()
async def uid(event: MessageEvent) -> Response:
    """
    Użycie:
        {command} (oznaczenie)
    Zwraca:
        twoje id lub oznaczonej osoby
    """

    if event.mentions:
        msg = event.mentions[0].thread_id
    else:
        msg = event.author_id

    return Response(event, text=msg)


# TODO: rewrite it in future XD
# TODO: real status instead of always positive
@MessageEventDispatcher.register(name='perm')
async def _perm(event: MessageEvent) -> Response:
    """
    Użycie:
        {command} look <nazwa>
        {command} <add/rem> <nazwa> <wl/bl> (tid=here/tid) <oznaczenia/uid>
    Zwraca:
        status, lub tablice permisji
    """

    msg = _perm.__doc__

    cmd = event.text.split(' ')
    if len(cmd) == 3:
        if cmd[1] == 'look':
            perms = PermissionRepository.find_by_command(cmd[2])
            if perms:
                msg = (
                    f'{ cmd[2] }:\n\n'
                    f'whitelist: { perms.whitelist }\n'
                    f'blacklist: { perms.blacklist }'
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
                    tid = event.thread_id

        bl = cmd[3] != 'wl'
        add = cmd[1] == 'add'

        # if remove from not existing permissions
        if not add and not cmd:
            return Response(event, text='Podana permisja nie istnieje')

        uids = cmd[3:]
        for mention in event.mentions:
            uids.append(mention.thread_id)

        msg = 'Pomyślnie '
        msg += 'dodano do ' if add else 'usunięto z '
        msg += 'blacklisty' if bl else 'whitelisty'

        perm.edit(cmd[2], uids, bl, add, tid)

    return Response(event, text=msg)


@MessageEventDispatcher.register()
async def ban(event: MessageEvent) -> Response:
    """
    Użycie:
        {command} <oznaczenie/uid>
    Zwraca:
        status
    """

    base = config.wiertarbot.prefix+'perm add banned wl '
    without_fw = event.text.split(' ', 1)[1]

    # fixme
    await _perm(
        MessageEvent.copy_with_different_text(event, base + without_fw)
    )

    return Response(event, text='Pomyślnie zbanowano')


@MessageEventDispatcher.register()
async def unban(event: MessageEvent) -> Response:
    """
    Użycie:
        {command} <oznaczenie/uid>
    Zwraca:
        status
    """

    base = config.wiertarbot.prefix+'perm rem banned wl '
    without_fw = event.text.split(' ', 1)[1]

    await _perm(
        MessageEvent.copy_with_different_text(event, base + without_fw)
    )

    return Response(event, text='Pomyślnie odbanowano')


@MessageEventDispatcher.register()
async def ile(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        ilość napisanych wiadomości od dodania bota
    """

    thread = await event.context.fetch_thread(event.thread_id)

    msg = f'Odkąd tutaj jestem napisano tu { thread.message_count } wiadomości.'

    return Response(event, text=msg)


@MessageEventDispatcher.register()
async def uptime(event: MessageEvent) -> Response:
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

    return Response(event, text=msg)


@MessageEventDispatcher.register()
async def see(event: MessageEvent) -> Optional[Response]:
    """
    Użycie:
        {command} (ilosc<=10)
    Zwraca:
        jedną lub więcej ostatnio usuniętych wiadomości w wątku
    """

    try:
        n = int(event.text.split(' ', 1)[1])
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

        response = Response(
            event,
            text=message['text'],
            mentions=mentions,
            files=files,
            voice_clip=voice_clip
        )
        send_responses.append(response.send())

    if send_responses:
        await asyncio.gather(*send_responses)
        return None
    else:
        return Response(event, text='Nie ma żadnych zapisanych usuniętych wiadomości w tym wątku')
