import requests
import random
import json
import os
from bs4 import BeautifulSoup
from io import BytesIO

from ..dispatch import MessageEventDispatcher
from ..events import MessageEvent
from ..response import Response
from .. import config


@MessageEventDispatcher.register()
async def suchar(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z sucharem
    """

    response = requests.get('http://www.suchary.com/random.html')
    parsed = BeautifulSoup(response.text, 'html.parser')
    image_url: str = parsed.find('div', 'file-container').a.img['src']  # type: ignore

    return event.response(files=[image_url])


@MessageEventDispatcher.register(aliases=['jeż'])
async def jez(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z jeżykiem
    """

    url = f'http://www.cutestpaw.com/tag/hedgehogs/page/{random.randint(1, 10)}/'
    response = requests.get(url)
    bs = BeautifulSoup(response.text, 'html.parser')
    h = bs.find_all('a', {'title': True})
    image_url = random.choice(h).img['src']  # type: ignore

    return event.response(files=[image_url])


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

    response = requests.get("https://unsplash.com/napi/search/photos", params=data).json()
    __zolw_pages = int(response["total_pages"])

    image_url = response["results"][0]["urls"]["regular"]

    image = BytesIO(
        requests.get(image_url).content
    )

    files_to_upload = [
        (f"turtle{random_page}.jpg", image, "image/jpeg")
    ]

    uploaded_files = await event.context.upload_raw(files_to_upload, False)
    await event.send_response(files=uploaded_files)


@MessageEventDispatcher.register(aliases=['dog', 'pies'])
async def doggo(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z pieskiem
    """

    response = requests.get('https://dog.ceo/api/breeds/image/random')
    data = json.loads(response.text)
    image_url = data['message']

    return event.response(files=[image_url])


@MessageEventDispatcher.register()
async def beagle(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z pieskiem rasy beagle
    """

    response = requests.get('https://dog.ceo/api/breed/beagle/images/random')
    data = json.loads(response.text)
    image_url = data['message']

    return event.response(files=[image_url])


@MessageEventDispatcher.register()
async def birb(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z ptaszkiem
    """

    response = requests.get('https://some-random-api.ml/img/birb')
    data = json.loads(response.text)
    image_url = data['link']

    return event.response(files=[image_url])


@MessageEventDispatcher.register()
async def wink(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        gif z mrugnięciem
    """

    response = requests.get('https://some-random-api.ml/animu/wink')
    data = json.loads(response.text)
    image_url = data['link']

    return event.response(files=[image_url])


if config.cat_api:
    __cat_api_key = config.cat_api.key

    @MessageEventDispatcher.register(aliases=['cat', 'kot'])
    async def catto(event: MessageEvent) -> Response:
        """
        Użycie:
            {command}
        Zwraca:
            zdjęcie z kotem
        """

        headers = {
            "x-api-key": __cat_api_key
        }

        response = requests.get('https://api.thecatapi.com/v1/images/search', headers=headers)
        data = json.loads(response.text)
        image_url = data[0]['url']

        return event.response(files=[image_url])


@MessageEventDispatcher.register(aliases=['panda'])
async def pandka(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z pandką
    """

    response = requests.get('https://some-random-api.ml/img/red_panda')
    data = json.loads(response.text)
    image_url = data['link']

    return event.response(files=[image_url])


@MessageEventDispatcher.register()
async def shiba(event: MessageEvent) -> Response:
    """
    Użycie:
        {command} (ilosc<=10)
    Zwraca:
        zdjęcie/a z pieskami rasy shiba
    """

    try:
        count = int(event.text.split(' ', 1)[1])
        if count > 10:
            count = 10
        elif count < 1:
            count = 1
    except (IndexError, ValueError):
        count = 1

    url = f'https://shibe.online/api/shibes?count={count}&urls=true&httpsUrls=true'
    response = requests.get(url)
    image_urls = json.loads(response.text)

    return event.response(files=image_urls)


def random_from_media_dir(directory: str) -> str:
    path = config.cmd_media_path / directory
    filename = random.choice(os.listdir(str(path)))
    return str(path / filename)


@MessageEventDispatcher.register()
async def konon(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z kononowiczem
    """

    image_path = random_from_media_dir('random/konon')

    return event.response(files=[image_path])


@MessageEventDispatcher.register()
async def papaj(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        cenzo
    """

    image_path = random_from_media_dir('random/papaj')

    return event.response(files=[image_path])


@MessageEventDispatcher.register()
async def bmw(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z rozwalonym bmw
    """

    image_path = random_from_media_dir('random/bmw')

    return event.response(files=[image_path])


@MessageEventDispatcher.register()
async def audi(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z rozwalonym audi
    """

    image_path = random_from_media_dir('random/audi')

    return event.response(files=[image_path])


@MessageEventDispatcher.register()
async def mikser(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z mikserem
    """

    image_path = random_from_media_dir('random/mikser')

    return event.response(files=[image_path])


@MessageEventDispatcher.register(aliases=['meme'])
async def mem(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        losowy mem
    Informacje:
        memy są z subredditów: memes, dankmemes, me_irl
    """

    # https://github.com/R3l3ntl3ss/Meme_Api
    r = requests.get('https://meme-api.herokuapp.com/gimme').text
    data = json.loads(r)

    msg = data['title']
    files = [data['url']]

    return event.response(text=msg, files=files)


@MessageEventDispatcher.register()
async def hug(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        losowy obrazek z tuleniem
    """

    response = requests.get("https://some-random-api.ml/animu/hug").json()

    return event.response(files=[response["link"]])


@MessageEventDispatcher.register()
async def jabol(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        Zdjęcie losowego jabola
    """

    image_path = random_from_media_dir('random/jabol')

    return event.response(files=[image_path])
