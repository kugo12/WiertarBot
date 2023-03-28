import requests
import random
from bs4 import BeautifulSoup

from ..message_dispatch import MessageEventDispatcher
from ..events import MessageEvent
from ..response import IResponse, file_upload_response


@MessageEventDispatcher.register()
async def suchar(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z sucharem
    """

    r = requests.get('http://www.suchary.com/random.html')
    parsed = BeautifulSoup(r.text, 'html.parser')
    image_url: str = parsed.find('div', 'file-container').a.img['src']  # type: ignore

    return await file_upload_response(event, [image_url])


@MessageEventDispatcher.register(aliases=['jeż'])
async def jez(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z jeżykiem
    """

    url = f'http://www.cutestpaw.com/tag/hedgehogs/page/{random.randint(1, 10)}/'
    r = requests.get(url)
    bs = BeautifulSoup(r.text, 'html.parser')
    h = bs.find_all('a', {'title': True})
    image_url = random.choice(h).img['src']  # type: ignore

    return await file_upload_response(event, [image_url])
