from typing import cast

import fbchat

from .. import perm
from ..message_dispatch import MessageEventDispatcher
from ..events import Mention, MessageEvent


@MessageEventDispatcher.register(special=True)
async def everyone(event: MessageEvent):
    if '@everyone' in event.text \
            and perm.check('everyone', event.thread_id, event.author_id) \
            and event.is_group:
        group = cast(fbchat.GroupData, await event.context.fetch_thread(event.thread_id))
        mentions = [
            Mention(thread_id=participant.id, offset=0, length=9)
            for participant in group.participants
        ]

        await event.send_response(text="@everyone", mentions=mentions)


@MessageEventDispatcher.register(special=True)
async def thinking(event: MessageEvent):
    if event.text == '🤔':
        await event.send_response(text='🤔')


@MessageEventDispatcher.register(special=True)
async def grek(event: MessageEvent):
    text = event.text.lower()
    if text == 'grek':
        if event.text == 'Grek':
            await event.send_response(text="grek*")
        await event.send_response(text="to pedał")
    elif text == 'pedał':
        await event.send_response(text="sam jesteś grek")
    elif text == 'pedał to':
        await event.send_response(text="grek")


@MessageEventDispatcher.register(special=True)
async def leet(event: MessageEvent):
    if '1337' in event.text:
        p = perm.check('leet', event.thread_id, event.author_id)

        await event.send_response(text="Jesteś elitą" if p else "Nie jesteś elitą")


@MessageEventDispatcher.register(special=True)
async def papiezowa_liczba(event: MessageEvent):
    if '2137' in event.text:
        await event.send_response(text='haha toż to papieżowa liczba')


@MessageEventDispatcher.register(special=True)
async def Xd_reaction(event: MessageEvent):
    if 'Xd' in event.text:
        await event.react('😠')  # angry reaction


async def spierwyp(event: MessageEvent, word: str):
    text = event.text.lower()
    msg = 'sam '

    if text.startswith('sam') and text.endswith(word):
        t = text.replace(' ', '').replace('sam', '').replace(word, '')
        if t == '' and text.count(word) == 1:
            msg = 'sam ' * (text.count('sam') + 1)

    if word in text:
        await event.send_response(text=msg + word)
        await event.react('😠')  # angry reaction


@MessageEventDispatcher.register(special=True)
async def spier(event: MessageEvent):
    await spierwyp(event, 'spierdalaj')


@MessageEventDispatcher.register(special=True)
async def wyp(event: MessageEvent):
    await spierwyp(event, 'wypierdalaj')
