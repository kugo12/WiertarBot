import fbchat
import random
import os
import requests
import baseconvert
import time
import json
from bs4 import BeautifulSoup
from datetime import datetime, date
from PIL import Image, ImageDraw, ImageFont
from io import BytesIO
from aiogtts import aiogTTS
from typing import Union
from aiogoogletrans import Translator

from ..dispatch import MessageEventDispatcher, Response
from ..bot import WiertarBot
from ..config import cmd_media_path, wb_site
from .modules import AliPaczka


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

    path = os.path.join(cmd_media_path, 'random/szkaluj.txt')
    with open(path, 'r', encoding='utf-8') as f:
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
        ("Koniec roku szkolnego (25 czerwca) za: ", datetime(2021, 6, 25)),
        ("Wielkanoc (4 kwietnia) za: ", datetime(2021, 4, 4))
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

# __mcd_r = [118, 121, 129, 145, 148, 151, 154, 157, 161, 170, 171, 173, 176, 181, 191, 199, 201, 215, 216, 220, 232, 236, 246, 249, 252, 253, 256, 257, 276, 322, 335, 337, 338, 349, 359, 362, 364, 375, 380, 384, 390, 396, 400, 421, 431, 435, 437, 438, 448, 450, 452, 454, 455, 468, 480, 481, 483, 484, 485, 487, 489, 490, 491, 492, 493, 494, 495, 496, 497, 498, 499]
# def __mcd_g():
#     while True:
#         re = random.randint(135, 535)
#         if re in __mcd_r:
#             pass
#         else:
#             return str(re)
__mcd_notr = ['135', '136', '137', '138', '139', '140', '141', '142', '143', '144', '146', '147', '149', '150', '152', '153', '155', '156', '158', '159', '160', '162', '163', '164', '165', '166', '167', '168', '169', '172', '174', '175', '177', '178', '179', '180', '182', '183', '184', '185', '186', '187', '188', '189', '190', '192', '193', '194', '195', '196', '197', '198', '200', '202', '203', '204', '205', '206', '207', '208', '209', '210', '211', '212', '213', '214', '217', '218', '219', '221', '222', '223', '224', '225', '226', '227', '228', '229', '230', '231', '233', '234', '235', '237', '238', '239', '240', '241', '242', '243', '244', '245', '247', '248', '250', '251', '254', '255', '258', '259', '260', '261', '262', '263', '264', '265', '266', '267', '268', '269', '270', '271', '272', '273', '274', '275', '277', '278', '279', '280', '281', '282', '283', '284', '285', '286', '287', '288', '289', '290', '291', '292', '293', '294', '295', '296', '297', '298', '299', '300', '301', '302', '303', '304', '305', '306', '307', '308', '309', '310', '311', '312', '313', '314', '315', '316', '317', '318', '319', '320', '321', '323', '324', '325', '326', '327', '328', '329', '330', '331', '332', '333', '334', '336', '339', '340', '341', '342', '343', '344', '345', '346', '347', '348', '350', '351', '352', '353', '354', '355', '356', '357', '358', '360', '361', '363', '365', '366', '367', '368', '369', '370', '371', '372', '373', '374', '376', '377', '378', '379', '381', '382', '383', '385', '386', '387', '388', '389', '391', '392', '393', '394', '395', '397', '398', '399', '401', '402', '403', '404', '405', '406', '407', '408', '409', '410', '411', '412', '413', '414', '415', '416', '417', '418', '419', '420', '422', '423', '424', '425', '426', '427', '428', '429', '430', '432', '433', '434', '436', '439', '440', '441', '442', '443', '444', '445', '446', '447', '449', '451', '453', '456', '457', '458', '459', '460', '461', '462', '463', '464', '465', '466', '467', '469', '470', '471', '472', '473', '474', '475', '476', '477', '478', '479', '482', '486', '488', '500', '501', '502', '503', '504', '505', '506', '507', '508', '509', '510', '511', '512', '513', '514', '515', '516', '517', '518', '519', '520', '521', '522', '523', '524', '525', '526', '527', '528', '529', '530', '531', '532', '533', '534', '535']


def __mcd_s(a: Union[int, str]) -> str:
    return ("0" + str(a))[-2:]


@MessageEventDispatcher.register()
async def mcd(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} <lody/hamburger>
    Zwraca:
        zdjęcie z kuponem do mcd
    """

    msg = mcd.__doc__
    files = None

    arg = event.message.text.split(' ', 1)
    if len(arg) == 2:
        arg = arg[1].lower()
        if arg in ['lody', 'hamburger']:
            if arg == 'lody':
                path = os.path.join(cmd_media_path, 'templates/kuponlody.jpg')
            else:
                path = os.path.join(cmd_media_path, 'templates/kuponhamburger.jpg')

            image = Image.open(path)

            t = time.time()-60
            d = datetime.fromtimestamp(t)

            d = [random.choice(__mcd_notr), __mcd_s(d.month), __mcd_s(d.day),
                 __mcd_s(d.hour), __mcd_s(d.minute),
                 '10', '05', str(random.randint(100, 999))]
            kod = int(''.join(d), 10)
            kod = baseconvert.base(kod, 10, 32, string=True)[0:11]

            draw = ImageDraw.Draw(image)
            try:
                path = os.path.join(cmd_media_path, 'arial.ttf')
                font = ImageFont.truetype(path, 16)
            except OSError:
                print('Wrzuc czcionke arial.ttf do WiertarBot/commands/media')
                return None

            date = time.strftime("%d-%m-%Y", time.gmtime(round(time.time())-86400))
            draw.text((3, 55), f'DATA WYDANIA { date }', font=font, fill="#000")
            tekst = "UNIKALNY KOD: 0"+kod
            size = draw.textsize(tekst, font=font)
            draw.text((577-size[0], 55), tekst, font=font, fill="#000")

            img_bin = BytesIO()
            image.save(img_bin, format='jpeg')
            image.close()
            img_bin.seek(0)

            fn = 'mcd.jpg'
            mime = 'image/jpeg'
            files = await WiertarBot.client.upload([(fn, img_bin, mime)])
            msg = None

    return Response(event, text=msg, files=files)


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


# constant
__covid_country_codes = ['af', 'al', 'dz', 'ad', 'ao', 'ag', 'ar', 'am', 'au', 'au', 'au', 'au', 'au', 'au', 'au', 'au', 'at', 'az', 'bs', 'bh', 'bd', 'bb', 'by', 'be', 'bj', 'bt', 'bo', 'ba', 'br', 'bn', 'bg', 'bf', 'cv', 'kh', 'cm', 'ca', 'ca', 'ca', 'ca', 'ca', 'ca', 'ca', 'ca', 'ca', 'ca', 'ca', 'cf', 'td', 'cl', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'cn', 'co', 'cg', 'cd', 'cr', 'ci', 'hr', 'xx', 'cu', 'cy', 'cz', 'dk', 'dk', 'dk', 'dj', 'do', 'ec', 'eg', 'sv', 'gq', 'er', 'ee', 'sz', 'et', 'fj', 'fi', 'fr', 'fr', 'fr', 'fr', 'fr', 'fr', 'fr', 'fr', 'fr', 'fr', 'ga', 'gm', 'ge', 'de', 'gh', 'gr', 'gt', 'gn', 'gy', 'ht', 'va', 'hn', 'hu', 'is', 'in', 'id', 'ir', 'iq', 'ie', 'il', 'it', 'jm', 'jp', 'jo', 'kz', 'ke', 'kr', 'kw', 'kg', 'lv', 'lb', 'lr', 'li', 'lt', 'lu', 'mg', 'my', 'mv', 'mt', 'mr', 'mu', 'mx', 'md', 'mc', 'mn', 'me', 'ma', 'na', 'np', 'nl', 'nl', 'nl', 'nl', 'nz', 'ni', 'ne', 'ng', 'mk', 'no', 'om', 'pk', 'pa', 'pg', 'py', 'pe', 'ph', 'pl', 'pt', 'qa', 'ro', 'ru', 'rw', 'lc', 'vc', 'sm', 'sa', 'sn', 'rs', 'sc', 'sg', 'sk', 'si', 'so', 'za', 'es', 'lk', 'sd', 'sr', 'se', 'ch', 'tw', 'tz', 'th', 'tg', 'tt', 'tn', 'tr', 'ug', 'ua', 'ae', 'gb', 'gb', 'gb', 'gb', 'gb', 'gb', 'gb', 'uy', 'us', 'uz', 've', 'vn', 'zm', 'zw', 'ca', 'dm', 'gd', 'mz', 'sy', 'tl', 'bz', 'la', 'ly', 'ps', 'gw', 'ml', 'kn', 'ca', 'ca', 'xk', 'mm', 'gb', 'gb', 'gb', 'xx', 'bw', 'bi', 'sl', 'nl', 'mw', 'gb', 'fr', 'ss', 'eh', 'st', 'ye', 'km', 'tj', 'ls']


@MessageEventDispatcher.register()
async def covid(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command} (s/w/kod kraju)
    Zwraca:
        informacje o koronawirusie
    Informacje:
        s - szczegółowe z Polski
        w - świat
        kody krajów - https://en.wikipedia.org/wiki/ISO_3166-2
    """

    msg = covid.__doc__

    args = event.message.text.split(' ', 1)
    if len(args) == 1:
        add = "http://35.205.149.154:5000/api"
        a = requests.get(add).text
        a = json.loads(a)
        n = a['new']
        ni = f'({ n["infected"] } nowych)' if n['infected'] else ''
        nd = f'({ n["deaths"] } nowych)' if n['deaths'] else ''
        # nr = f'({ n["recovered"] } nowych)' if n['recovered'] else ''

        msg = (
            f'Statystyki COVID19 w Polsce na ten moment:\n'
            f'{ a["infected"] } chorych { ni }\n'
            f'{ a["deaths"] } śmierci { nd }\n'
            # f'{ a['recovered'] } wyleczonych { nr }\n'
            f'szczegółowe dane Polski !covid s\n'
            f'dane świata !covid w\n'
            f'dane innych krajów !covid <kod kraju>'
        )

    elif len(args) == 2:
        arg = args[1].lower()
        if arg == 's':
            add = "http://35.205.149.154:5000/api/detailed"
            a = requests.get(add).text
            a = json.loads(a)
            msg = ''
            for i in a:
                n = a[i]['new']
                ni = f'({ n["infected"] } nowych)' if n['infected'] else ''
                nd = f'({ n["deaths"] } nowych)' if n['deaths'] else ''
                # nr = f'({ n["recovered"] } nowych)' if n['recovered'] else ''

                msg += (
                    f'{ i }:\n'
                    f' { a[i]["infected"] } chorych { ni }\n'
                    f' { a[i]["deaths"] } śmierci { nd }\n'
                    # f'{ a[i]["recovered"] } wyleczonych { nr }\n'
                )

        elif arg == 'w':
            url = 'https://coronavirus-tracker-api.herokuapp.com/v2/latest?timelines=false'
            data = requests.get(url).text
            data = json.loads(data)

            latest = data['latest']
            msg = (
                f'Informacje dla świata\n'
                f'{ latest["confirmed"] } zakażeń\n'
                f'{ latest["deaths"] } zgonów'
            )
            recovered = f'\n{ latest["recovered"] } wyleczonych' if latest['recovered'] else ''
            msg += recovered

        elif arg in __covid_country_codes:
            url = (f'https://coronavirus-tracker-api.herokuapp.com/v2/'
                   f'locations?country_code={ arg }&timelines=false')
            data = requests.get(url).text
            data = json.loads(data)

            latest = data['latest']
            msg = (
                f'Informacje dla { data["locations"][0]["country"] }\n'
                f'{ latest["confirmed"] } zakażeń\n'
                f'{ latest["deaths"] } zgonów'
            )
            recovered = f'\n{ latest["recovered"] } wyleczonych' if latest['recovered'] else ''
            msg += recovered

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
    date(2021, 1, 31),
    date(2021, 3, 28),
    date(2021, 4, 25),
    date(2021, 6, 27),
    date(2021, 8, 29),
    date(2021, 12, 12),
    date(2021, 12, 19)
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
