import requests
import random
import json
import os
from bs4 import BeautifulSoup
from io import BytesIO

from ..message_dispatch import MessageEventDispatcher
from ..events import MessageEvent
from ..response import IResponse, response
from .. import config


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

    return response(event, files=[image_url])


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

    return response(event, files=[image_url])


__zolw_pages = 1000


@MessageEventDispatcher.register(aliases=['żółw'])
async def zolw(event: MessageEvent):
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z żółwikiem
    """

    global __zolw_pages

    random_page = random.randint(1, __zolw_pages)
    data = {
        "query": "turtle",
        "per_page": "1",
        "page": str(random_page)
    }

    r = requests.get("https://unsplash.com/napi/search/photos", params=data).json()
    __zolw_pages = int(r["total_pages"])

    image_url = r["results"][0]["urls"]["regular"]

    image = BytesIO(
        requests.get(image_url).content
    )

    files_to_upload = [
        (f"turtle{random_page}.jpg", image, "image/jpeg")
    ]

    uploaded_files = await event.getContext().upload_raw(files_to_upload, False)
    await response(event, files=uploaded_files).pySend()


@MessageEventDispatcher.register(aliases=['dog', 'pies'])
async def doggo(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z pieskiem
    """

    r = requests.get('https://dog.ceo/api/breeds/image/random')
    data = json.loads(r.text)
    image_url = data['message']

    return response(event, files=[image_url])


@MessageEventDispatcher.register()
async def beagle(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z pieskiem rasy beagle
    """

    r = requests.get('https://dog.ceo/api/breed/beagle/images/random')
    data = json.loads(r.text)
    image_url = data['message']

    return response(event, files=[image_url])


@MessageEventDispatcher.register()
async def birb(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z ptaszkiem
    """

    r = requests.get('https://some-random-api.ml/img/birb')
    data = json.loads(r.text)
    image_url = data['link']

    return response(event, files=[image_url])


@MessageEventDispatcher.register()
async def wink(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command}
    Zwraca:
        gif z mrugnięciem
    """

    r = requests.get('https://some-random-api.ml/animu/wink')
    data = json.loads(r.text)
    image_url = data['link']

    return response(event, files=[image_url])


if config.cat_api:
    __cat_api_key = config.cat_api.key

    @MessageEventDispatcher.register(aliases=['cat', 'kot'])
    async def catto(event: MessageEvent) -> IResponse:
        """
        Użycie:
            {command}
        Zwraca:
            zdjęcie z kotem
        """

        headers = {
            "x-api-key": __cat_api_key
        }

        r = requests.get('https://api.thecatapi.com/v1/images/search', headers=headers)
        data = json.loads(r.text)
        image_url = data[0]['url']

        return response(event, files=[image_url])


@MessageEventDispatcher.register(aliases=['panda'])
async def pandka(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z pandką
    """

    r = requests.get('https://some-random-api.ml/img/red_panda')
    data = json.loads(r.text)
    image_url = data['link']

    return response(event, files=[image_url])


@MessageEventDispatcher.register()
async def shiba(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command} (ilosc<=10)
    Zwraca:
        zdjęcie/a z pieskami rasy shiba
    """

    try:
        count = int(event.getText().split(' ', 1)[1])
        if count > 10:
            count = 10
        elif count < 1:
            count = 1
    except (IndexError, ValueError):
        count = 1

    url = f'https://shibe.online/api/shibes?count={count}&urls=true&httpsUrls=true'
    image_urls = requests.get(url).json()

    return response(event, files=image_urls)


def random_from_media_dir(directory: str) -> str:
    path = config.cmd_media_path / directory
    filename = random.choice(os.listdir(str(path)))
    return str(path / filename)


@MessageEventDispatcher.register()
async def konon(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z kononowiczem
    """

    image_path = random_from_media_dir('random/konon')

    return response(event, files=[image_path])


@MessageEventDispatcher.register()
async def papaj(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command}
    Zwraca:
        cenzo
    """

    image_path = random_from_media_dir('random/papaj')

    return response(event, files=[image_path])


@MessageEventDispatcher.register()
async def bmw(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z rozwalonym bmw
    """

    image_path = random_from_media_dir('random/bmw')

    return response(event, files=[image_path])


@MessageEventDispatcher.register()
async def audi(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z rozwalonym audi
    """

    image_path = random_from_media_dir('random/audi')

    return response(event, files=[image_path])


@MessageEventDispatcher.register()
async def mikser(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z mikserem
    """

    image_path = random_from_media_dir('random/mikser')

    return response(event, files=[image_path])


@MessageEventDispatcher.register(aliases=['meme'])
async def mem(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command}
    Zwraca:
        losowy mem
    Informacje:
        memy są z subredditów: memes, dankmemes, me_irl
    """

    # https://github.com/R3l3ntl3ss/Meme_Api
    data = requests.get('https://meme-api.herokuapp.com/gimme').json()

    msg = data['title']
    files = [data['url']]

    return response(event, text=msg, files=files)


@MessageEventDispatcher.register()
async def hug(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command}
    Zwraca:
        losowy obrazek z tuleniem
    """

    data = requests.get("https://some-random-api.ml/animu/hug").json()

    return response(event, files=[data["link"]])


@MessageEventDispatcher.register()
async def jabol(event: MessageEvent) -> IResponse:
    """
    Użycie:
        {command}
    Zwraca:
        Zdjęcie losowego jabola
    """

    image_path = random_from_media_dir('random/jabol')

    return response(event, files=[image_path])
