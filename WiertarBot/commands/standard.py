from typing import Optional, cast

import fbchat
import random
import requests
import json
import asyncio
from decimal import Decimal
from bs4 import BeautifulSoup
from datetime import datetime, date
from io import BytesIO
from aiogtts import aiogTTS
from aiogoogletrans import Translator
from forex_python.converter import convert as currency_convert, RatesNotAvailableError

from ..message_dispatch import MessageEventDispatcher
from ..events import MessageEvent, Mention
from ..response import Response
from ..config import cmd_media_path, wiertarbot as wiertarbot_config
from .modules import AliPaczka, Fantano


@MessageEventDispatcher.register()
async def wybierz(event: MessageEvent) -> Response:
    """
    Użycie:
        {command} <opcje do wyboru po przecinku>
    Zwraca:
        losowo wybrana opcje
    """

    txt = "Brak opcji do wyboru"
    m = event.text.split(" ", 1)
    if len(m) > 1:
        m = m[1].split(",")
        txt = random.choice(m)

    return event.response(text=txt)


@MessageEventDispatcher.register()
async def moneta(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        wynik rzutu monetą (orzeł lub reszka)
    """

    if random.getrandbits(1):
        msg = 'Orzeł!'
    else:
        msg = 'Reszka!'

    return event.response(text=msg)


@MessageEventDispatcher.register()
async def kostka(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        wynik rzutu kostką
    """

    n = random.randint(1, 6)
    msg = f'Wyrzuciłeś {n}'

    return event.response(text=msg)


@MessageEventDispatcher.register()
async def szkaluj(event: MessageEvent) -> Response:
    """
    Użycie:
        {command} (oznaczenie/random)
    Zwraca:
        tekst szkalujący osobę
    """

    text = event.text.lower()
    is_group_and_random = (
            event.is_group
            and text.count(' ') == 1
            and text.endswith(' random')
    )
    if is_group_and_random:
        thread = cast(fbchat.GroupData, await event.context.fetch_thread(event.thread_id))
        uid = random.choice(list(thread.participants)).id
    elif event.mentions:
        uid = event.mentions[0].thread_id
    else:
        uid = event.author_id

    user = await event.context.fetch_thread(uid)
    name = str(user.name)

    path = cmd_media_path / 'random/szkaluj.txt'
    with path.open('r', encoding='utf-8') as f:
        lines = f.readlines()
        msg = random.choice(lines)
        del lines

    msg = msg.replace('%n%', '\n')

    mentions = []
    while '%on%' in msg:
        mention = Mention(thread_id=uid, offset=msg.find('%on%'), length=len(name))
        mentions.append(mention)
        msg = msg.replace('%on%', name, 1)

    return event.response(text=msg, mentions=mentions)


@MessageEventDispatcher.register()
async def donate(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        link do pp
    """

    msg = (
        'https://paypal.me/kugo12\n'
        'z góry dzięki'
    )

    return event.response(text=msg)


@MessageEventDispatcher.register()
async def changelog(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        link do spisu zmian bota
    """

    return event.response(text='https://github.com/kugo12/WiertarBot/commits/main')


@MessageEventDispatcher.register()
async def kod(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        link do kodu bota
    """

    msg = 'https://github.com/kugo12/WiertarBot'

    return event.response(text=msg)


@MessageEventDispatcher.register()
async def barka(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        tekst barki
    """

    msg = 'Pan kiedyś stanął nad brzegiem\nSzukał ludzi gotowych pójść za Nim\nBy łowić serca\nSłów Bożych prawdą.\n\nRef.:\nO Panie, to Ty na mnie spojrzałeś,\nTwoje usta dziś wyrzekły me imię.\nSwoją barkę pozostawiam na brzegu,\nRazem z Tobą nowy zacznę dziś łów.\n\n2.\nJestem ubogim człowiekiem,\nMoim skarbem są ręce gotowe\nDo pracy z Tobą\nI czyste serce.\n\n3.\nTy, potrzebujesz mych dłoni,\nMego serca młodego zapałem\nMych kropli potu\nI samotności.\n\n4.\nDziś wypłyniemy już razem\nŁowić serca na morzach dusz ludzkich\nTwej prawdy siecią\nI słowem życia.\n\n\nBy Papież - https://www.youtube.com/watch?v=fimrULqiExA\nZ tekstem - https://www.youtube.com/watch?v=_o9mZ_DVTKA'

    return event.response(text=msg)


__Xd_message = 'Serio, mało rzeczy mnie triggeruje tak jak to chore \"Xd\". Kombinacji x i d można używać na wiele wspaniałych sposobów. Coś cię śmieszy? Stawiasz \"xD\". Coś się bardzo śmieszy? Śmiało: \"XD\"! Coś doprowadza Cię do płaczu ze śmiechu? \"XDDD\" i załatwione. Uśmiechniesz się pod nosem? \"xd\". Po kłopocie. A co ma do tego ten bękart klawiaturowej ewolucji, potwór i zakała ludzkiej estetyki - \"Xd\"? Co to w ogóle ma wyrażać? Martwego człowieka z wywalonym jęzorem? Powiem Ci, co to znaczy. To znaczy, że masz w telefonie włączone zaczynanie zdań dużą literą, ale szkoda Ci klikać capsa na jedno \"d\" później. Korona z głowy spadnie? Nie sondze. \"Xd\" to symptom tego, że masz mnie, jako rozmówcę, gdzieś, bo Ci się nawet kliknąć nie chce, żeby mi wysłać poprawny emotikon. Szanujesz mnie? Używaj \"xd\", \"xD\", \"XD\", do wyboru. Nie szanujesz mnie? Okaż to. Wystarczy, że wstawisz to zjebane \"Xd\" w choć jednej wiadomości. Nie pozdrawiam'


@MessageEventDispatcher.register(aliases=['xd'])
async def Xd(event: MessageEvent) -> Optional[Response]:
    """
    Użycie:
        !Xd
    Zwraca:
        copypaste o Xd
    """

    return event.response(text=__Xd_message) if event.text == '!Xd' else None


# constants
__czas_localize_datetime = {
    "January": "Stycznia",
    "February": "Lutego",
    "March": "Marca",
    "April": "Kwietnia",
    "May": "Maja",
    "June": "Czerwca",
    "July": "Lipca",
    "August": "Sierpnia",
    "September": "Września",
    "October": "Października",
    "November": "Listopada",
    "December": "Grudnia",
    "Monday": "Poniedziałek",
    "Tuesday": "Wtorek",
    "Wednesday": "Środa",
    "Thursday": "Czwartek",
    "Friday": "Piątek",
    "Saturday": "Sobota",
    "Sunday": "Niedziela"
}
__czas_timers = [
    ("Początek wakacji (23 czerwca) za: ", datetime(2023, 6, 23)),
    ("Początek \"wakacji\" dla maturzystów (28 kwietnia) za: ", datetime(2023, 4, 28)),
]


@MessageEventDispatcher.register()
async def czas(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        aktualny czas oraz odliczania
    """

    now = datetime.now(wiertarbot_config.tz)
    now_str = now.strftime("%A %d %B %H:%M")

    for i in __czas_localize_datetime:
        if i in now_str:
            now_str = now_str.replace(i, __czas_localize_datetime[i])

    msg = f'Jest: {now_str}'

    for timer in __czas_timers:
        timedelta = timer[1] - now

        if timedelta.days > -1:
            d = timedelta.days
            h = timedelta.seconds // 3600
            m = (timedelta.seconds // 60) % 60
            s = int(timedelta.seconds % 60)

            msg += f'\n{timer[0]}: {d}d {h}h {m}min {s}sek'

    return event.response(text=msg)


@MessageEventDispatcher.register()
async def track(event: MessageEvent) -> Response:
    """
    Użycie:
        {command} <numer śledzenia>
    Zwraca:
        status paczki
    """

    msg = track.__doc__

    arg = event.text.split(' ', 1)
    if len(arg) == 2:
        msg = str(AliPaczka(arg[1]))

    return event.response(text=msg)


@MessageEventDispatcher.register(aliases=['słownik'])
async def slownik(event: MessageEvent) -> Response:
    """
    Użycie:
        {command} <wyraz>
    Zwraca:
        definicję podanego wyrazu z sjp.pwn.pl
    """

    msg = slownik.__doc__

    args = event.text.split(' ', 1)
    if len(args) == 2:
        arg = args[1].lower()
        url = f'https://sjp.pwn.pl/slowniki/{arg.replace(" ", "-")}'

        response = requests.get(url).text
        parsed = BeautifulSoup(response, 'html.parser')
        text = parsed.body.find('div', {'class': 'ribbon-element type-187126'})  # type: ignore
        if text:
            msg = text.get_text().strip()
        else:
            msg = 'Coś poszło nie tak, jak nie użyłeś polskich liter, to dobry moment'

    return event.response(text=msg)


@MessageEventDispatcher.register()
async def miejski(event: MessageEvent) -> Response:
    """
    Użycie:
        {command} <wyraz>
    Zwraca:
        definicję podanego wyrazu z www.miejski.pl
    """

    msg = miejski.__doc__

    args = event.text.split(' ', 1)
    if len(args) == 2:
        arg = args[1].lower()
        url = f'https://www.miejski.pl/slowo-{arg.replace(" ", "+")}'

        response = requests.get(url)
        if response.status_code == 404:
            msg = 'Nie znaleziono takiego słowa'
        else:
            parsed = BeautifulSoup(response.text, 'html.parser')
            main = parsed.body.find('main')  # type: ignore

            definition = main.find('p').get_text()  # type: ignore

            example = main.find('blockquote')  # type: ignore
            example = f'\n\nPrzyklad/y: {example.get_text()}' if example else ''  # type: ignore

            msg = f'{arg}\nDefinicja: {definition}{example}'

    return event.response(text=msg)


# constants
__tts_gtts = aiogTTS()


@MessageEventDispatcher.register()
async def tts(event: MessageEvent) -> Response:
    """
    Użycie:
        {command} (lang=kod) <tekst>
    Zwraca:
        wiadomość głosową text-to-speech
    Informacje:
        spis kodów języków na wiertarbot.pl/tts
    Przykład:
        {command} lang=en hello world
    """

    msg = tts.__doc__
    files = None

    arg = event.text.split(' ', 2)
    if len(arg) > 1:
        lang = 'pl'
        if arg[1].startswith('lang='):
            if len(arg) == 2:
                return event.response(text=msg)

            lang = arg[1].replace('lang=', '')
            arg[1] = ''

        text = ''.join(arg[1:])

        f = BytesIO()
        await __tts_gtts.write_to_fp(text, f, lang=lang)
        f.seek(0)

        fn = 'tts.mp3'
        mime = 'audio/mp3'
        files = await event.context.upload_raw([(fn, f, mime)], voice_clip=False)
        msg = None

    return event.response(text=msg, files=files)


@MessageEventDispatcher.register()
async def mc(event: MessageEvent) -> Response:
    """
    Użycie:
        {command} <names/skin> <nick>
    Zwraca:
        skin lub historie nazw związana z nickiem
    """

    msg = mc.__doc__
    files = None

    args = event.text.split(' ', 2)
    if len(args) == 3:
        arg = args[1].lower()

        uuid = requests.get(f'https://api.mojang.com/users/profiles/minecraft/{args[2]}').text
        if uuid:
            uuid = json.loads(uuid)["id"]

            if arg == 'names':
                names = requests.get(f'https://api.mojang.com/user/profiles/{uuid}/names').text
                names = json.loads(names)

                msg = f'Oryginalny: {names[0]["name"]}\n'  # type: ignore
                for name in names[1:]:
                    date = datetime.fromtimestamp(int(name['changedToAt']) / 1000)  # type: ignore
                    msg += f'{date}: {name["name"]}\n'  # type: ignore

            elif arg == 'skin':
                files = [
                    f'https://crafatar.com/skins/{uuid}.png',
                    f'https://crafatar.com/renders/body/{uuid}.png?overlay&scale=6',
                    f'https://crafatar.com/avatars/{uuid}.png',
                    f'https://crafatar.com/renders/head/{uuid}.png?overlay&scale=6'
                ]
                msg = None

        else:
            msg = 'Podany nick nie istnieje'

    return event.response(text=msg, files=files)


@MessageEventDispatcher.register()
async def covid(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        informacje o koronawirusie
    """

    msg = covid.__doc__

    args = event.text.split(' ', 1)
    if len(args) == 1:
        url = "https://services-eu1.arcgis.com/zk7YlClTgerl62BY/arcgis/rest/services/global_corona_actual_widok3/FeatureServer/0/query?f=json&cacheHint=true&resultOffset=0&resultRecordCount=1&where=1%3D1&outFields=*"
        response = requests.get(url).json()
        fields = response["features"][0]["attributes"]

        msg = (
            f'Statystyki COVID19 w Polsce na {fields["DATA_SHOW"]}:\n'
            'Dziennie:\n'
            f'{fields["ZAKAZENIA_DZIENNE"]} zakażonych\n'
            f'{fields["ZGONY_DZIENNE"]} zgonów\n'
            f'{fields["LICZBA_OZDROWIENCOW"]} ozdrowieńców\n'
            f'{fields["TESTY"]} testów\n'
            f'{fields["TESTY_POZYTYWNE"]} testów pozytywnych\n'
            f'{fields["KWARANTANNA"]} osób na kwarantannie aktualnie\n'

            '\nOgółem:\n'
            f'{fields["LICZBA_ZAKAZEN"]} zakażonych\n'
            f'{fields["WSZYSCY_OZDROWIENCY"]} ozdrowieńców\n'
            f'{fields["LICZBA_ZGONOW"]} zgonów'
            # f'szczegółowe dane Polski !covid s\n'
        )

    return event.response(text=msg)


@MessageEventDispatcher.register()
async def sugestia(event: MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        https://github.com/kugo12/WiertarBot/issues
    """

    return event.response(text="https://github.com/kugo12/WiertarBot/issues")


translator = Translator()


@MessageEventDispatcher.register(aliases=['tłumacz'])
async def tlumacz(event: MessageEvent) -> Response:
    """
    Użycie:
        {command} <docelowy język> <tekst>
    Zwraca:
        przetłumaczony tekst
    Informacje:
    """

    msg = tlumacz.__doc__

    args = event.text.split(' ', 2)
    if len(args) == 3:
        try:
            t = await translator.translate(args[2], dest=args[1])
            msg = t.text
        except ValueError:
            msg = 'Zły docelowy język'

    return event.response(text=msg)


sundays = [
    date(2023, 1, 29),
    date(2023, 4, 2),
    date(2023, 4, 30),
    date(2023, 6, 25),
    date(2023, 8, 27),
    date(2023, 12, 17),
    date(2023, 12, 24),
]


@MessageEventDispatcher.register()
async def niedziela(event: MessageEvent) -> Response:
    """
    Użycie:
        {command} [lista]
    Zwraca:
        Najbliższą niedzielę handlową

        {prefix}{command} lista
        Zwraca listę niedziel handlowych w tym roku
    """

    now = date.today()
    msg = niedziela.__doc__

    request = event.text.split(" ", 2)
    if len(request) == 1:
        nearest_sunday = None

        for s in sundays:
            if s >= now:
                nearest_sunday = s
                break

        if nearest_sunday:
            msg = "Dzisiejsza niedziela jest handlowa"
            if now != nearest_sunday:
                msg = f"Najbliższa handlowa niedziela: {nearest_sunday.isoformat()}"

    elif request[1] == "lista":
        msg = "Niedziele handlowe:\n- " \
              + "\n- ".join([i.isoformat() for i in sundays if i >= now])

    return event.response(text=msg)


@MessageEventDispatcher.register(aliases=['anthony', 'melon'])
async def fantano(event: MessageEvent) -> Response:
    """
    Użycie:
        {command} <nazwa albumu>
    Zwraca:
        Nazwa albumu: <Nazwa Albumu>
        Treść: <Treść recenzji>
        Ocena: <Końcowa ocena>
    """
    args = event.text.split(' ', 1)
    if len(args) == 2:
        review = Fantano().get_rate(args[1])
        msg = (
            f"Nazwa albumu: {review['title']}\n"
            f"Treść: {review['review']}\n"
            f"Ocena: {review['rate']}"
        )
    else:
        msg = fantano.__doc__

    return event.response(text=msg)


def __convert_currency(_from: str, to: str, amount: Decimal) -> Decimal:
    return cast(Decimal, currency_convert(_from, to, amount))


@MessageEventDispatcher.register(aliases=["przelicz"])
async def kurs(event: MessageEvent) -> Response:
    """
    Użycie:
        {command} <z waluty> <do waluty> (ilosc=1)
    Zwraca:
        Obecny kurs Forex
    """

    args = event.text.split(" ")
    msg = kurs.__doc__

    if len(args) in (3, 4):
        try:
            currency_from = args[1].upper()
            currency_to = args[2].upper()
            amount = Decimal(args[3] if len(args) == 4 else 1)

            converted = await asyncio.to_thread(__convert_currency, currency_from, currency_to, amount)

            msg = f"{amount} {currency_from} to {converted:.4f} {currency_to}"
        except ValueError:
            msg = "Ostatni argument (ilość) nie jest liczbą"
        except RatesNotAvailableError:
            msg = "Nieprawidłowa waluta"

    return event.response(text=msg)
