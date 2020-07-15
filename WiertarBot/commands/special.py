import fbchat

from .. import perm
from ..dispatch import MessageEventDispatcher
from ..bot import WiertarBot


@MessageEventDispatcher.register(special=True)
async def everyone(event: fbchat.MessageEvent):
    if '@everyone' in event.message.text:
        if perm.check('everyone', event.thread.id, event.author.id):
            if isinstance(event.thread, fbchat.Group):
                group = await WiertarBot.client.fetch_thread_info([event.thread.id]).__anext__()
                mentions = [
                    fbchat.Mention(thread_id=participant.id, offset=0, length=9)
                    for participant in group.participants
                ]

                await event.thread.send_text('@everyone', mentions=mentions)


@MessageEventDispatcher.register(special=True)
async def thinking(event: fbchat.MessageEvent):
    if event.message.text == '🤔':
        await event.thread.send_text('🤔')


@MessageEventDispatcher.register(special=True)
async def grek(event: fbchat.MessageEvent):
    text = event.message.text.lower()
    if text == 'grek':
        if event.message.text == 'Grek':
            await event.thread.send_text('grek*')
        await event.thread.send_text('to pedał')
    elif text == 'pedał':
        await event.thread.send_text('sam jesteś grek')
    elif text == 'pedał to':
        await event.thread.send_text('grek')


@MessageEventDispatcher.register(special=True)
async def leet(event: fbchat.MessageEvent):
    if '1337' in event.message.text:
        if perm.check('leet', event.thread.id, event.author.id):
            await event.thread.send_text('Jesteś elitą')
        else:
            await event.thread.send_text('Nie jesteś elitą')


@MessageEventDispatcher.register(special=True)
async def papiezowa_liczba(event: fbchat.MessageEvent):
    if '2137' in event.message.text:
        await event.thread.send_text('haha toż to papieżowa liczba')


@MessageEventDispatcher.register(special=True)
async def Xd_reaction(event: fbchat.MessageEvent):
    if 'Xd' in event.message.text:
        await event.message.react('😠')  # angry reaction


async def spierwyp(event: fbchat.MessageEvent, word: str):
    text = event.message.text.lower()
    msg = 'sam '

    if text.startswith('sam') and text.endswith(word):

        t = text.replace(' ', '').replace('sam', '').replace(word, '')
        if t == '' and text.count(word) == 1:
            msg = 'sam ' * (text.count('sam') + 1)

    if word in text:
        await event.thread.send_text(msg + word)
        await event.message.react('😠')  # angry reaction


@MessageEventDispatcher.register(special=True)
async def spier(event: fbchat.MessageEvent):
    await spierwyp(event, 'spierdalaj')


@MessageEventDispatcher.register(special=True)
async def wyp(event: fbchat.MessageEvent):
    await spierwyp(event, 'wypierdalaj')
