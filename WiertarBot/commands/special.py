from typing import cast

import fbchat

from ..message_dispatch import MessageEventDispatcher
from ..events import Mention, MessageEvent
from ..response import response
from ..services import PermissionService


@MessageEventDispatcher.register(special=True)
async def everyone(event: MessageEvent):
    if '@everyone' in event.getText() \
            and PermissionService.isAuthorized('everyone', event.getThreadId(), event.getAuthorId()) \
            and event.isGroup():
        group = cast(fbchat.GroupData, await event.getContext().fetch_thread(event.getThreadId()))
        mentions = [
            Mention(thread_id=participant.id, offset=0, length=9)
            for participant in group.participants
        ]

        await response(event, text="@everyone", mentions=mentions).pySend()


@MessageEventDispatcher.register(special=True)
async def thinking(event: MessageEvent):
    if event.getText() == '🤔':
        await response(event, text='🤔').pySend()


@MessageEventDispatcher.register(special=True)
async def grek(event: MessageEvent):
    text = event.getText().lower()
    if text == 'grek':
        if event.getText() == 'Grek':
            await response(event, text="grek*").pySend()
        await response(event, text="to pedał").pySend()
    elif text == 'pedał':
        await response(event, text="sam jesteś grek").pySend()
    elif text == 'pedał to':
        await response(event, text="grek").pySend()


@MessageEventDispatcher.register(special=True)
async def leet(event: MessageEvent):
    if '1337' in event.getText():
        p = PermissionService.isAuthorized('leet', event.getThreadId(), event.getAuthorId())

        await response(event, text="Jesteś elitą" if p else "Nie jesteś elitą").pySend()


@MessageEventDispatcher.register(special=True)
async def papiezowa_liczba(event: MessageEvent):
    if '2137' in event.getText():
        await response(event, text='haha toż to papieżowa liczba').pySend()


@MessageEventDispatcher.register(special=True)
async def Xd_reaction(event: MessageEvent):
    if 'Xd' in event.getText():
        await event.pyReact('😠')  # angry reaction


async def spierwyp(event: MessageEvent, word: str):
    text = event.getText().lower()
    msg = 'sam '

    if text.startswith('sam') and text.endswith(word):
        t = text.replace(' ', '').replace('sam', '').replace(word, '')
        if t == '' and text.count(word) == 1:
            msg = 'sam ' * (text.count('sam') + 1)

    if word in text:
        await response(event, text=msg + word).pySend()
        await event.pyReact('😠')  # angry reaction


@MessageEventDispatcher.register(special=True)
async def spier(event: MessageEvent):
    await spierwyp(event, 'spierdalaj')


@MessageEventDispatcher.register(special=True)
async def wyp(event: MessageEvent):
    await spierwyp(event, 'wypierdalaj')
