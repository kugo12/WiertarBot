import aiohttp
import fbchat
import json
import asyncio
from io import BytesIO
from typing import List, Awaitable, Iterable

from .. import perm
from ..dispatch import MessageEventDispatcher
from ..response import Response
from ..config import prefix, attachment_save_path
from ..bot import WiertarBot
from ..database import FBMessage


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
            perms = perm.get_permission(cmd[2])
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

        perms = perm.get_permission(cmd[2])
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


@MessageEventDispatcher.register()
async def ile(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        ilość napisanych wiadomości od dodania bota
    """

    thread = await WiertarBot.client.fetch_thread_info([event.thread.id]).__anext__()

    msg = f'Odkąd tutaj jestem napisano tu { thread.message_count } wiadomości.'

    return Response(event, text=msg)


@MessageEventDispatcher.register()
async def uptime(event: fbchat.MessageEvent) -> Response:
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
async def prof(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} (oznaczenie)
    Zwraca:
        twoje zdjęcie profilowe lub oznaczonej osoby
    """

    if event.message.mentions:
        uid = event.message.mentions[0].thread_id
    else:
        uid = event.author.id

    url = f'https://graph.facebook.com/v3.1/{ uid }/picture?height=500'
    mime = 'image/jpeg'
    fn = 'prof.jpg'

    async with aiohttp.ClientSession() as session:
        async with session.get(url) as r:
            if r.status == 200:
                f = BytesIO(await r.read())

    files = await WiertarBot.client.upload([(fn, f, mime)])

    return Response(event, files=files)


@MessageEventDispatcher.register()
async def see(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} (ilosc<=10)
    Zwraca:
        jedną lub więcej ostatnio usuniętych wiadomości w wątku
    """

    try:
        n = int(event.message.text.split(' ', 1)[1])
        if n > 10:
            n = 10
        elif n < 1:
            n = 1
    except (IndexError, ValueError):
        n = 1

    messages: Iterable[FBMessage] = FBMessage\
        .select(FBMessage.message)\
        .where(
            FBMessage.deleted_at != None,
            FBMessage.thread_id == event.thread.id
        )\
        .order_by(FBMessage.time.desc())\
        .limit(n)

    send_responses: List[Awaitable] = []
    for message in messages:
        message = json.loads(message.message)

        mentions = [
            fbchat.Mention(**mention)
            for mention in message['mentions']
        ]

        voice_clip = False
        files = []
        for att in message['attachments']:
            if att['type'] == 'ImageAttachment':
                p = attachment_save_path / f'{ att["id"] }.{ att["original_extension"] }'
                files.append(str(p))
            elif att['type'] == 'AudioAttachment':
                p = attachment_save_path / att['filename']
                files.append(str(p))
                voice_clip = True
            elif att['type'] == 'VideoAttachment':
                p = attachment_save_path / f'{ att["id"] }.mp4'
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
    else:
        return Response(event, text='Nie ma żadnych zapisanych usuniętych wiadomości w tym wątku')
