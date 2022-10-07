from typing import cast

import fbchat

from .. import perm
from ..dispatch import MessageEventDispatcher
from ..events import Mention, MessageEvent
from ..response import Response


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

        await Response(
            event=event,
            text="@everyone",
            mentions=mentions
        ).send()


@MessageEventDispatcher.register(special=True)
async def thinking(event: MessageEvent):
    if event.text == '🤔':
        await Response(event=event, text='🤔').send()


@MessageEventDispatcher.register(special=True)
async def grek(event: MessageEvent):
    text = event.text.lower()
    if text == 'grek':
        if event.text == 'Grek':
            await Response(event=event, text="grek*").send()
        await Response(event=event, text="to pedał").send()
    elif text == 'pedał':
        await Response(event=event, text="sam jesteś grek").send()
    elif text == 'pedał to':
        await Response(event=event, text="grek").send()


@MessageEventDispatcher.register(special=True)
async def leet(event: MessageEvent):
    if '1337' in event.text:
        p = perm.check('leet', event.thread_id, event.author_id)

        await Response(event=event, text="Jesteś elitą" if p else "Nie jesteś elitą").send()


@MessageEventDispatcher.register(special=True)
async def papiezowa_liczba(event: MessageEvent):
    if '2137' in event.text:
        await Response(event=event, text='haha toż to papieżowa liczba').send()


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
        await Response(
            event=event,
            text=msg + word
        ).send()
        await event.react('😠')  # angry reaction


@MessageEventDispatcher.register(special=True)
async def spier(event: MessageEvent):
    await spierwyp(event, 'spierdalaj')


@MessageEventDispatcher.register(special=True)
async def wyp(event: MessageEvent):
    await spierwyp(event, 'wypierdalaj')
