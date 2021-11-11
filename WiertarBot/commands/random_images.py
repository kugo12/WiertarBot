import fbchat
import requests
import random
import json
import os
from bs4 import BeautifulSoup
from io import BytesIO

from ..dispatch import MessageEventDispatcher
from ..response import Response
from .. import config
from ..bot import WiertarBot


@MessageEventDispatcher.register()
async def suchar(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z sucharem
    """

    response = requests.get('http://www.suchary.com/random.html')
    parsed = BeautifulSoup(response.text, 'html.parser')
    image_url = parsed.body.find('div', 'file-container').a.img['src']

    return Response(event, files=[image_url])


@MessageEventDispatcher.register(aliases=['jeż'])
async def jez(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z jeżykiem
    """

    url = f'http://www.cutestpaw.com/tag/hedgehogs/page/{ random.randint(1, 10) }/'
    response = requests.get(url)
    h = BeautifulSoup(response.text, 'html.parser')
    h = h.find_all('a', {'title': True})
    image_url = random.choice(h).img['src']

    return Response(event, files=[image_url])


@MessageEventDispatcher.register(aliases=['żółw'])
async def zolw(event: fbchat.MessageEvent):
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z żółwikiem
    """

    random_page = random.randint(1, zolw.pages)
    data = {
        "query": "turtle",
        "per_page": 1,
        "page": random_page
    }

    response = requests.get("https://unsplash.com/napi/search/photos", params=data).text
    response = json.loads(response)
    zolw.pages = response["total_pages"]

    image_url = response["results"][0]["urls"]["regular"]

    image = BytesIO(
        requests.get(image_url).content
    )

    files_to_upload = [
        (f"turtle{ random_page }.jpg", image, "image/jpeg")
    ]

    uploaded_files = await WiertarBot.client.upload(files_to_upload, False)
    await event.thread.send_files(uploaded_files)
zolw.pages = 1000


@MessageEventDispatcher.register(aliases=['dog', 'pies'])
async def doggo(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z pieskiem
    """

    response = requests.get('https://dog.ceo/api/breeds/image/random')
    data = json.loads(response.text)
    image_url = data['message']

    return Response(event, files=[image_url])


@MessageEventDispatcher.register()
async def beagle(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z pieskiem rasy beagle
    """

    response = requests.get('https://dog.ceo/api/breed/beagle/images/random')
    data = json.loads(response.text)
    image_url = data['message']

    return Response(event, files=[image_url])


@MessageEventDispatcher.register()
async def birb(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z ptaszkiem
    """

    response = requests.get('https://some-random-api.ml/img/birb')
    data = json.loads(response.text)
    image_url = data['link']

    return Response(event, files=[image_url])


@MessageEventDispatcher.register()
async def wink(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        gif z mrugnięciem
    """

    response = requests.get('https://some-random-api.ml/animu/wink')
    data = json.loads(response.text)
    image_url = data['link']

    return Response(event, files=[image_url])


@MessageEventDispatcher.register(aliases=['cat', 'kot'])
async def catto(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z kotem
    """

    response = requests.get('https://api.thecatapi.com/v1/images/search', config.thecatapi_headers)
    data = json.loads(response.text)
    image_url = data[0]['url']

    return Response(event, files=[image_url])


@MessageEventDispatcher.register(aliases=['panda'])
async def pandka(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z pandką
    """

    response = requests.get('https://some-random-api.ml/img/red_panda')
    data = json.loads(response.text)
    image_url = data['link']

    return Response(event, files=[image_url])


@MessageEventDispatcher.register()
async def shiba(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} (ilosc<=10)
    Zwraca:
        zdjęcie/a z pieskami rasy shiba
    """

    try:
        count = int(event.message.text.split(' ', 1)[1])
        if count > 10:
            count = 10
        elif count < 1:
            count = 1
    except (IndexError, ValueError):
        count = 1

    url = f'https://shibe.online/api/shibes?count={ count }&urls=true&httpsUrls=true'
    response = requests.get(url)
    image_urls = json.loads(response.text)

    return Response(event, files=image_urls)


def random_from_media_dir(directory: str) -> str:
    path = config.cmd_media_path / directory
    filename = random.choice(os.listdir(str(path)))
    return str(path / filename)


@MessageEventDispatcher.register()
async def konon(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z kononowiczem
    """

    image_path = random_from_media_dir('random/konon')

    return Response(event, files=[image_path])


@MessageEventDispatcher.register()
async def papaj(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        cenzo
    """

    image_path = random_from_media_dir('random/papaj')

    return Response(event, files=[image_path])


@MessageEventDispatcher.register()
async def bmw(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z rozwalonym bmw
    """

    image_path = random_from_media_dir('random/bmw')

    return Response(event, files=[image_path])

@MessageEventDispatcher.register()
async def audi(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z rozwalonym audi
    """

    image_path = random_from_media_dir('random/audi')

    return Response(event, files=[image_path])

@MessageEventDispatcher.register()
async def mikser(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        zdjęcie z mikserem
    """

    image_path = random_from_media_dir('random/mikser')

    return Response(event, files=[image_path])


@MessageEventDispatcher.register(aliases=['meme'])
async def mem(event: fbchat.MessageEvent) -> Response:
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

    return Response(event, text=msg, files=files)


@MessageEventDispatcher.register()
async def hug(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        losowy obrazek z tuleniem
    """

    response = requests.get("https://some-random-api.ml/animu/hug").json()

    return Response(event, files=[response["link"]])

@MessageEventDispatcher.register()
async def jabol(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        Zdjęcie losowego jabola
    """

    image_path = random_from_media_dir('random/jabol')

    return Response(event, files=[image_path])