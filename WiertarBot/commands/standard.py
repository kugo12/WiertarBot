import fbchat
import random
import requests
import json
from bs4 import BeautifulSoup
from datetime import datetime, date
from io import BytesIO
from aiogtts import aiogTTS
from aiogoogletrans import Translator

from ..dispatch import MessageEventDispatcher
from ..response import Response
from ..bot import WiertarBot
from ..config import cmd_media_path, wb_site
from .modules import AliPaczka, Fantano


@MessageEventDispatcher.register()
async def wybierz(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} <opcje do wyboru po przecinku>
    Zwraca:
        losowo wybrana opcje
    """

    txt = "Brak opcji do wyboru"
    m = event.message.text.split(" ", 1)
    if len(m) > 1:
        m = m[1].split(",")
        txt = random.choice(m)

    return Response(event, text=txt)


@MessageEventDispatcher.register()
async def moneta(event: fbchat.MessageEvent) -> Response:
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

    return Response(event, text=msg)


@MessageEventDispatcher.register()
async def kostka(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        wynik rzutu kostką
    """

    n = random.randint(1, 6)
    msg = f'Wyrzuciłeś { n }'

    return Response(event, text=msg)


@MessageEventDispatcher.register()
async def szkaluj(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} (oznaczenie/random)
    Zwraca:
        tekst szkalujący osobę
    """

    text = event.message.text.lower()
    is_group_and_random = (
        event.thread.id != event.author.id
        and text.count(' ') == 1
        and text.endswith(' random')
    )
    if is_group_and_random:
        thread = await WiertarBot.client.fetch_thread_info([event.thread.id]).__anext__()
        uid = random.choice(thread.participants).id
    elif event.message.mentions:
        uid = event.message.mentions[0].thread_id
    else:
        uid = event.author.id

    user = await WiertarBot.client.fetch_thread_info([uid]).__anext__()
    name = user.name

    path = cmd_media_path / 'random/szkaluj.txt'
    with path.open('r', encoding='utf-8') as f:
        lines = f.readlines()
        msg = random.choice(lines)
        del lines

    msg = msg.replace('%n%', '\n')

    mentions = []
    while '%on%' in msg:
        mention = fbchat.Mention(thread_id=uid, offset=msg.find('%on%'), length=len(name))
        mentions.append(mention)
        msg = msg.replace('%on%', name, 1)

    return Response(event, text=msg, mentions=mentions)


@MessageEventDispatcher.register()
async def donate(event: fbchat.MessageEvent) -> Response:
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

    return Response(event, text=msg)


@MessageEventDispatcher.register()
async def changelog(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        link do spisu zmian bota
    """

    msg = 'https://wiertarbot.pl/changelog'

    return Response(event, text=msg)


@MessageEventDispatcher.register()
async def kod(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        link do kodu bota
    """

    msg = 'https://github.com/kugo12/WiertarBot'

    return Response(event, text=msg)


@MessageEventDispatcher.register()
async def barka(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        tekst barki
    """

    msg = 'Pan kiedyś stanął nad brzegiem\nSzukał ludzi gotowych pójść za Nim\nBy łowić serca\nSłów Bożych prawdą.\n\nRef.:\nO Panie, to Ty na mnie spojrzałeś,\nTwoje usta dziś wyrzekły me imię.\nSwoją barkę pozostawiam na brzegu,\nRazem z Tobą nowy zacznę dziś łów.\n\n2.\nJestem ubogim człowiekiem,\nMoim skarbem są ręce gotowe\nDo pracy z Tobą\nI czyste serce.\n\n3.\nTy, potrzebujesz mych dłoni,\nMego serca młodego zapałem\nMych kropli potu\nI samotności.\n\n4.\nDziś wypłyniemy już razem\nŁowić serca na morzach dusz ludzkich\nTwej prawdy siecią\nI słowem życia.\n\n\nBy Papież - https://www.youtube.com/watch?v=fimrULqiExA\nZ tekstem - https://www.youtube.com/watch?v=_o9mZ_DVTKA'

    return Response(event, text=msg)


@MessageEventDispatcher.register(aliases=['xd'])
async def Xd(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        !Xd
    Zwraca:
        copypaste o Xd
    """

    if event.message.text == '!Xd':
        msg = 'Serio, mało rzeczy mnie triggeruje tak jak to chore \"Xd\". Kombinacji x i d można używać na wiele wspaniałych sposobów. Coś cię śmieszy? Stawiasz \"xD\". Coś się bardzo śmieszy? Śmiało: \"XD\"! Coś doprowadza Cię do płaczu ze śmiechu? \"XDDD\" i załatwione. Uśmiechniesz się pod nosem? \"xd\". Po kłopocie. A co ma do tego ten bękart klawiaturowej ewolucji, potwór i zakała ludzkiej estetyki - \"Xd\"? Co to w ogóle ma wyrażać? Martwego człowieka z wywalonym jęzorem? Powiem Ci, co to znaczy. To znaczy, że masz w telefonie włączone zaczynanie zdań dużą literą, ale szkoda Ci klikać capsa na jedno \"d\" później. Korona z głowy spadnie? Nie sondze. \"Xd\" to symptom tego, że masz mnie, jako rozmówcę, gdzieś, bo Ci się nawet kliknąć nie chce, żeby mi wysłać poprawny emotikon. Szanujesz mnie? Używaj \"xd\", \"xD\", \"XD\", do wyboru. Nie szanujesz mnie? Okaż to. Wystarczy, że wstawisz to zjebane \"Xd\" w choć jednej wiadomości. Nie pozdrawiam'

        return Response(event, text=msg)


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
        ("Koniec wakacji (1 wrzesnia) za: ", datetime(2021, 9, 1))
]


@MessageEventDispatcher.register()
async def czas(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        aktualny czas oraz odliczania
    """

    now = datetime.now()
    now_str = now.strftime("%A %d %B %H:%M")

    for i in __czas_localize_datetime:
        if i in now_str:
            now_str = now_str.replace(i, __czas_localize_datetime[i])

    msg = f'Jest: { now_str }'

    for timer in __czas_timers:
        timedelta = timer[1] - now

        if timedelta.days > -1:
            d = timedelta.days
            h = timedelta.seconds // 3600
            m = (timedelta.seconds // 60) % 60
            s = int(timedelta.seconds % 60)

            msg += f'\n{ timer[0] }: { d }d { h }h { m }min { s }sek'

    return Response(event, text=msg)


@MessageEventDispatcher.register()
async def track(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} <numer śledzenia>
    Zwraca:
        status paczki
    """

    msg = track.__doc__

    arg = event.message.text.split(' ', 1)
    if len(arg) == 2:
        msg = AliPaczka(arg[1])

    return Response(event, text=msg)


@MessageEventDispatcher.register(aliases=['słownik'])
async def slownik(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} <wyraz>
    Zwraca:
        definicję podanego wyrazu z sjp.pwn.pl
    """

    msg = slownik.__doc__

    arg = event.message.text.split(' ', 1)
    if len(arg) == 2:
        arg = arg[1].lower()
        url = f'https://sjp.pwn.pl/slowniki/{ arg.replace(" ", "-") }'

        response = requests.get(url).text
        parsed = BeautifulSoup(response, 'html.parser')
        text = parsed.body.find('div', {'class': 'ribbon-element type-187126'})
        if text:
            msg = text.get_text().strip()
        else:
            msg = 'Coś poszło nie tak, jak nie użyłeś polskich liter, to dobry moment'

    return Response(event, text=msg)


@MessageEventDispatcher.register()
async def miejski(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} <wyraz>
    Zwraca:
        definicję podanego wyrazu z www.miejski.pl
    """

    msg = miejski.__doc__

    arg = event.message.text.split(' ', 1)
    if len(arg) == 2:
        arg = arg[1].lower()
        url = f'https://www.miejski.pl/slowo-{ arg.replace(" ", "+") }'

        response = requests.get(url)
        if response.status_code == 404:
            msg = 'Nie znaleziono takiego słowa'
        else:
            parsed = BeautifulSoup(response.text, 'html.parser')
            main = parsed.body.find('main')

            definition = main.find('p').get_text()

            example = main.find('blockquote')
            example = f'\n\nPrzyklad/y: { example.get_text() }' if example else ''

            msg = f'{ arg }\nDefinicja: { definition }{ example }'

    return Response(event, text=msg)


# constants
__tts_gtts = aiogTTS()


@MessageEventDispatcher.register()
async def tts(event: fbchat.MessageEvent) -> Response:
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

    arg = event.message.text.split(' ', 2)
    if len(arg) > 1:
        lang = 'pl'
        if arg[1].startswith('lang='):
            if len(arg) == 2:
                return Response(event, text=msg)

            lang = arg[1].replace('lang=', '')
            arg[1] = ''

        text = ''.join(arg[1:])

        f = BytesIO()
        await __tts_gtts.write_to_fp(text, f, lang=lang)
        f.seek(0)

        fn = 'tts.mp3'
        mime = 'audio/mp3'
        files = await WiertarBot.client.upload([(fn, f, mime)], voice_clip=True)
        msg = None

    return Response(event, text=msg, files=files)


@MessageEventDispatcher.register()
async def mc(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} <names/skin> <nick>
    Zwraca:
        skin lub historie nazw związana z nickiem
    """

    msg = mc.__doc__
    files = None

    args = event.message.text.split(' ', 2)
    if len(args) == 3:
        arg = args[1].lower()

        uuid = requests.get(f'https://api.mojang.com/users/profiles/minecraft/{ args[2] }').text
        if uuid:
            uuid = json.loads(uuid)["id"]

            if arg == 'names':
                names = requests.get(f'https://api.mojang.com/user/profiles/{ uuid }/names').text
                names = json.loads(names)

                msg = f'Oryginalny: { names[0]["name"] }\n'
                for name in names[1:]:
                    date = datetime.fromtimestamp(name['changedToAt']/1000)
                    msg += f'{ date }: { name["name"] }\n'

            elif arg == 'skin':
                files = [
                    f'https://crafatar.com/skins/{ uuid }.png',
                    f'https://crafatar.com/renders/body/{ uuid }.png?overlay&scale=6',
                    f'https://crafatar.com/avatars/{ uuid }.png',
                    f'https://crafatar.com/renders/head/{ uuid }.png?overlay&scale=6'
                ]
                msg = None

        else:
            msg = 'Podany nick nie istnieje'

    return Response(event, text=msg, files=files)


@MessageEventDispatcher.register()
async def covid(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        informacje o koronawirusie
    """

    msg = covid.__doc__

    args = event.message.text.split(' ', 1)
    if len(args) == 1:
        url = "https://services-eu1.arcgis.com/zk7YlClTgerl62BY/arcgis/rest/services/global_corona_actual_widok3/FeatureServer/0/query?f=json&cacheHint=true&resultOffset=0&resultRecordCount=1&where=1%3D1&outFields=*"
        response = requests.get(url).json()
        fields = response["features"][0]["attributes"]

        msg = (
            f'Statystyki COVID19 w Polsce na { fields["DATA_SHOW"] }:\n'
            'Dziennie:\n'
            f'{ fields["ZAKAZENIA_DZIENNE"] } zakażonych\n'
            f'{ fields["ZGONY_DZIENNE"] } zgonów\n'
            f'{ fields["LICZBA_OZDROWIENCOW"] } ozdrowieńców\n'
            f'{ fields["TESTY"] } testów\n'
            f'{ fields["TESTY_POZYTYWNE"] } testów pozytywnych\n'
            f'{ fields["KWARANTANNA"] } osób na kwarantannie aktualnie\n'

            '\nOgółem:\n'
            f'{ fields["LICZBA_ZAKAZEN"] } zakażonych\n'
            f'{ fields["WSZYSCY_OZDROWIENCY"] } ozdrowieńców\n'
            f'{ fields["LICZBA_ZGONOW"] } zgonów'
            # f'szczegółowe dane Polski !covid s\n'
        )

    return Response(event, text=msg)


@MessageEventDispatcher.register()
async def sugestia(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} (tekst, minimum dwa wyrazy)
    Zwraca:
        link do strony z dodaną sugestią
    Link do sugestii:
        https://wiertarbot.pl/sugestie/
    """

    msg = sugestia.__doc__

    args = event.message.text.split(' ', 3)
    if len(args) > 2 and ' ' not in args:
        txt = ' '.join(args[1:])
        name = await WiertarBot.client.fetch_thread_info(event.author.id).__anext__()
        name = name.name

        data = {
            'api': wb_site['api_key'],
            'text': txt,
            'name': name
        }

        response = requests.post(url=wb_site['add_suggestion_url'], data=data)

        if response.status_code == 200:
            url = json.loads(response.text)['url']
            msg = f'Sugestia pomyślnie dodana, znajduje się pod adresem:\nhttps://{ url }'

        else:
            msg = 'Coś poszło nie tak'

    return Response(event, text=msg)


translator = Translator()


@MessageEventDispatcher.register(aliases=['tłumacz'])
async def tlumacz(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} <docelowy język> <tekst>
    Zwraca:
        przetłumaczony tekst
    Informacje:
    """

    msg = tlumacz.__doc__

    args = event.message.text.split(' ', 2)
    if len(args) == 3:
        try:
            t = await translator.translate(args[2], dest=args[1])
            msg = t.text
        except ValueError:
            msg = 'Zły docelowy język'

    return Response(event, text=msg)


sundays = [
    date(2022, 6, 26),
    date(2022, 8, 28),
    date(2022, 12, 11),
    date(2022, 12, 18)
]
    

@MessageEventDispatcher.register()
async def niedziela(event: fbchat.MessageEvent) -> Response:
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

    request = event.message.text.split(" ", 2)
    if len(request) == 1:
        nearest_sunday = None

        for s in sundays:
            if s >= now:
                nearest_sunday = s
                break

        if nearest_sunday:
            msg = "Dzisiejsza niedziela jest handlowa"
            if now != nearest_sunday:
                msg = f"Najbliższa handlowa niedziela: { nearest_sunday.isoformat() }"

    elif request[1] == "lista":
        msg = "Niedziele handlowe:\n- " \
            + "\n- ".join([i.isoformat() for i in sundays if i >= now])

    return Response(event, text=msg)

@MessageEventDispatcher.register(aliases=['anthony', 'melon'])
async def fantano(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} <nazwa albumu>
    Zwraca:
        Nazwa albumu: <Nazwa Albumu>
        Treść: <Treść recenzji>
        Ocena: <Końcowa ocena>
    """
    args = event.message.text.split(' ', 1)
    if len(args) == 2:
        review = Fantano().get_rate(args[1])
        msg = (
            f"Nazwa albumu: {review['title']}\n"
            f"Treść: {review['review']}\n"
            f"Ocena: {review['rate']}"
        )
    else:
        msg = fantano.__doc__

    return Response(event, text=msg)
