from fbchat import Client, ThreadColor, ThreadType, Mention, Message, MessageReaction
from bs4 import BeautifulSoup
from pickle import dumps, loads, load, dump
from gtts import gTTS
import logging
from PIL import Image, ImageDraw, ImageFont, ImageOps, ImageEnhance, ImageFilter
import base64, random, time, datetime, json, requests, os, sys, sqlite3, baseconvert, schedule, asyncio, subprocess

from alipaczka import AliPaczka
from mobidziennik import MobiDziennik

try:
    import local_config as config
except ModuleNotFoundError:
    import config

# move working dir to location of bot.py
abspath = os.path.abspath(__file__)
dname = os.path.dirname(abspath)
os.chdir(dname)

# create necessary directories
# todo: directory structure really needs rework
dirs = [
    'bmw',
    'cenzo',
    'konon',
    'mikser',
    config.attachments_location,
    config.attachments_location+'tts/',
    config.attachments_location+'kupony/',
    config.attachments_location+'templates/',
]

for d in dirs:
    if os.path.isdir(d) == False:
        os.mkdir(d)

# todo: remove
# old code hardcoded things
colors = [ThreadColor.BILOBA_FLOWER, ThreadColor.BRILLIANT_ROSE, ThreadColor.CAMEO, ThreadColor.DEEP_SKY_BLUE, ThreadColor.FERN, ThreadColor.FREE_SPEECH_GREEN, ThreadColor.GOLDEN_POPPY, ThreadColor.LIGHT_CORAL, ThreadColor.MEDIUM_SLATE_BLUE, ThreadColor.MESSENGER_BLUE]
meine = "2311754082235146" # class group
mojeid = "100002268938732"

class WiertarBot(Client):
    standard = {}
    special = []
    conn: sqlite3.Connection
    cur: sqlite3.Cursor
    help_text = "Prefix komend: "+config.cmd_prefix+"\nDostępne komendy:\nhelp, uid, tid, perm, barka, czas, ile, Xd, kod, mem, miejski, suchar, doggo, catto, birb, shiba, bmw, papaj, pandka, tencza, tts, mcd, n, moneta, kostka, konon, wypierdalaj, 2020, deepfry, prof, uptime, szkaluj, download, pikachu, mc, changelog, beagle, jez, zolw, covid, sugestia, donate"
    img_query = {}
    uptime = round(time.time())

    def dailyNumber(self):
        if datetime.datetime.today().weekday() > 4:
            return 0
        n = str(int(self.mb.getLuckyNumber())) # idk
        try:
            who = config.my_class[n]
        except:
            return True
        msg = "Dzisiaj szczęśliwy numerek ma: "+who["name"]+" ("+str(n)+")"
        
        mnt = [Mention(who["uid"], 31, len(who["name"]))]
        self.loop.create_task(self.send(Message(msg, mnt), meine, ThreadType.GROUP))

    async def on_listen_error(self, exception=None):
        print(exception)
        if not await self.is_logged_in():
            await self.login(config.email, config.password)

    # todo: make image editing class based
    async def standard_nobody(self, command, args, query_args=None):
        if len(command) >= 1:
            if command[0] == "img":
                # try:
                if "who" in query_args:
                    url = "https://graph.facebook.com/v3.1/"+str(query_args["who"])+"/picture?height=500"
                else:
                    url = await self.fetch_image_url(args["message_object"].attachments[0].uid)

                if "text" in query_args:
                    text = query_args["text"]
                else:
                    text = "Nobody:\n\nMe:   "
                
                # except:
                    # j = check_request(self._get(ReqUrl.ATTACHMENT_PHOTO, query={"photo_id": str(args["message_object"].attachments[0].uid)}))["redirect"]
                    # j = self._session.get(j, headers=self._header, verify=self.ssl_verify, allow_redirects=True)
                    # b = str(j.text)
                    # print(b)
                    # url = b[b.find("[\"http")+2:b.find("dl=1\"")+4].replace("\/", "/")
                # print("\n"+str(url)+"\n")
                img = requests.get(url).content
                fn = config.attachments_location+args["message_object"].uid+'_edit.jpg'
                open(fn, 'wb').write(img)
                #edit
                img = Image.open(fn).convert("RGB")
                font = ImageFont.truetype("arial.ttf", 44)
                draw = ImageDraw.Draw(img)
                w, h = draw.textsize(text, font=font)
                h = h+22
                w = round(w*1.7)
                tps = [w, h]
                tmpl = Image.new("RGB", tps, "#fff")
                draw = ImageDraw.Draw(tmpl)
                draw.text((0,0), text, font=font, fill="#000")
                ims = [img.width, img.height]

                if tps[0] >= ims[0]:
                    h = round(tps[0]*ims[1]/ims[0])
                    ims = (tps[0], h)
                    new = Image.new("RGB", [tps[0], h+tps[1]])
                    sz = (0, tps[1])
                else:
                    h = round(ims[0]*tps[1]/tps[0])
                    new = Image.new("RGB", [ims[0], ims[1]+h])
                    tmpl = tmpl.resize((ims[0], h))
                    ims = (ims[0], ims[1])
                    sz = (0, h)

                new.paste(tmpl, (0, 0))
                img = img.resize(ims)
                new.paste(img, sz)

                new.save(fn, quality=97)

                await self.send_local_files([fn], None, args["thread_id"], args["thread_type"])
                os.remove(fn)
                return True

            elif command[1][:4] == "who=":
                if command[1][4:] == "@me":
                    qa = {"who":args["author_id"]}
                    if len(command) > 2:
                        qa["text"] = "Nobody:\n\n"+args["message_object"].text[16:]+": "

                    self.loop.create_task(self.standard_nobody(["img"], args, qa))
                    return True
                elif len(args["message_object"].mentions) == 1:
                    m = args["message_object"].mentions[0]
                    qa = {"who":m.thread_id}
                    if len(command) > 2:
                        qa["text"] = "Nobody:\n\n"+args["message_object"].text[m.offset+m.length+1:]+": "
                    
                    self.loop.create_task(self.standard_nobody(["img"], args, qa))
                    return True
                else:
                    await self.send(Message("Użycie:\n"+config.cmd_prefix+"nobody who=(@me lub oznaczenie) <tekst>\nGenerator memów z nobody."), args["thread_id"], args["thread_type"])
                    return True
            else:
                if len(command) == 1:
                    qa = None
                else:
                    qa = {"text":"Nobody:\n\n"+args["message_object"].text[8:]+": "}
                if args["message_object"].reply_to_id != None:
                    try:
                        a = await self.fetch_message_info(args["message_object"].reply_to_id, args["thread_id"])
                        if a.attachments[0].__class__.__name__ == "ImageAttachment":
                            args["message_object"] = a
                            self.loop.create_task(self.standard_nobody(["img"], args, qa))
                            return True
                    except:
                        pass
                    await self.send(Message("Wiadomość jest bez zdjęcia"))
                    return True

                t = round(time.time()) + 120
                if (args["thread_id"] in self.img_query) == False:
                    self.img_query[args["thread_id"]] = {}
                self.img_query[args["thread_id"]][args["author_id"]] = [self.standard_nobody, t, qa]
                await self.send(Message("Wyślij zdjęcie"), args["thread_id"], args["thread_type"])
                return True
                
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"nobody who=(@me lub oznaczenie) <tekst>\nGenerator memów z nobody."), args["thread_id"], args["thread_type"])

    async def standard_deepfry(self, command, args, query_args=None):
        if len(command) == 1:
            if command[0] == "img":
                # try:
                if query_args == None:
                    url = await self.fetch_image_url(args["message_object"].attachments[0].uid)
                else:
                    url = "https://graph.facebook.com/v3.1/"+str(query_args)+"/picture?height=500"
                # except:
                    # j = check_request(self._get(ReqUrl.ATTACHMENT_PHOTO, query={"photo_id": str(args["message_object"].attachments[0].uid)}))["redirect"]
                    # j = self._session.get(j, headers=self._header, verify=self.ssl_verify, allow_redirects=True)
                    # b = str(j.text)
                    # print(b)
                    # url = b[b.find("[\"http")+2:b.find("dl=1\"")+4].replace("\/", "/")
                # print("\n"+str(url)+"\n")
                img = requests.get(url).content
                fn = config.attachments_location+args["message_object"].uid+'_edit.jpg'
                open(fn, 'wb').write(img)

                #edit
                img = Image.open(fn).convert("RGB")
                fl = Image.open("saved/templates/flara.png").convert("RGBA")
                fl2 = Image.open("saved/templates/flara2.png").convert("RGBA")
                fl = fl.resize((round(2*fl.width*fl.width/img.width), round(2*fl.height*fl.height/img.height)))
                fl2 = fl2.resize((round(2*fl2.width*fl2.width/img.width), round(2*fl2.height*fl2.height/img.height)))
                off = (round(fl.width/2), round(fl.height/2))
                fl = ImageEnhance.Contrast(fl).enhance(2)
                # fl = ImageEnhance.Sharpness(fl).enhance(100)
                # fl = ImageEnhance.Brightness(fl).enhance(1.5)
                # fl = fl.filter(ImageFilter.GaussianBlur(2))
                
                coile = 20
                a = []
                for i in range(int(img.width/coile)):
                    for j in range(int(img.height/coile)):
                        xy = (i*coile, j*coile)
                        if img.getpixel(xy) == (240, 53, 36):
                            a.append(xy)
                # for i in a:
                #     pos = (i[0]-off[0], i[1]-off[1])
                #     img.paste(fl, pos, fl)

                img = ImageOps.posterize(img, 4)

                r = img.split()[0]
                r = ImageEnhance.Contrast(r).enhance(2.0)
                r = ImageEnhance.Brightness(r).enhance(1.5)
                r = ImageOps.colorize(r, (254, 0, 2), (255, 255, 15))
                img = Image.blend(img, r, 0.6)

                for i in a:
                    pos = (i[0]-off[0], i[1]-off[1])
                    img.paste(fl, pos, fl)
                for i in a:
                    pos = (i[0]-off[0], i[1]-off[1])
                    img.paste(fl2, pos, fl2)
                img = ImageEnhance.Sharpness(img).enhance(10.0)
                img = ImageEnhance.Contrast(img).enhance(1.5)

                img.save(fn, quality=50)

                await self.send_local_files([fn], None, args["thread_id"], args["thread_type"])
                os.remove(fn)
            elif args["message_object"].reply_to_id != None:
                try:
                    a = await self.fetch_message_info(args["message_object"].reply_to_id, args["thread_id"])
                    if a.attachments[0].__class__.__name__ == "ImageAttachment":
                        args["message_object"] = a
                        self.loop.create_task(self.standard_deepfry(["img"], args))
                        return True
                except:
                    pass
                await self.send(Message("Wiadomość jest bez zdjęcia"))
                return True
            else:
                t = round(time.time()) + 120
                if (args["thread_id"] in self.img_query) == False:
                    self.img_query[args["thread_id"]] = {}
                self.img_query[args["thread_id"]][args["author_id"]] = [self.standard_deepfry, t, None]
                await self.send(Message("Wyślij zdjęcie"), args["thread_id"], args["thread_type"])
            return True
        elif len(command) > 1:
            if command[1] == "@me":
                self.loop.create_task(self.standard_deepfry(["img"], args, args["author_id"]))
                return True
            else:
                if len(args["message_object"].mentions) > 0:
                    self.loop.create_task(self.standard_deepfry(["img"], args, args["message_object"].mentions[0].thread_id))
                    return True
                
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"deepfry (@me lub oznaczenie)\nSmaży zdjęcie i dodaje flary (czerwony kolor)."), args["thread_id"], args["thread_type"])
        
    async def standard_2020(self, command, args, query_args=None):
        if len(command) == 1:
            if command[0] == "img":
                # try:
                if query_args == None:
                    url = await self.fetch_image_url(args["message_object"].attachments[0].uid)
                else:
                    url = "https://graph.facebook.com/v3.1/"+str(query_args)+"/picture?height=500"
                # except:
                    # j = check_request(self._get(ReqUrl.ATTACHMENT_PHOTO, query={"photo_id": str(args["message_object"].attachments[0].uid)}))["redirect"]
                    # j = self._session.get(j, headers=self._header, verify=self.ssl_verify, allow_redirects=True)
                    # b = str(j.text)
                    # print(b)
                    # url = b[b.find("[\"http")+2:b.find("dl=1\"")+4].replace("\/", "/")
                # print("\n"+str(url)+"\n")
                img = requests.get(url).content
                fn = config.attachments_location+args["message_object"].uid+'_edit.jpg'
                open(fn, 'wb').write(img)

                #edit
                tps = [500, 179]
                img = Image.open(fn)
                ims = [img.width, img.height]
                tmpl = Image.open("saved/templates/2019.jpg")

                if tps[0] >= ims[0]:
                    h = round(500*ims[1]/ims[0])
                    ims = (500, h)
                    new = Image.new("RGB", [500, h+179])
                    sz = (0, tps[1])
                else:
                    h = round(ims[0]*tps[1]/tps[0])
                    new = Image.new("RGB", [ims[0], ims[1]+h])
                    tmpl = tmpl.resize((ims[0], h))
                    ims = (ims[0], ims[1])
                    sz = (0, h)
               
                new.paste(tmpl, (0, 0))
                img = img.resize(ims)
                new.paste(img, sz)
                
                new.save(fn, quality=97)

                await self.send_local_files([fn], None, args["thread_id"], args["thread_type"])
                os.remove(fn)
            elif args["message_object"].reply_to_id != None:
                try:
                    a = await self.fetch_message_info(args["message_object"].reply_to_id, args["thread_id"])
                    if a.attachments[0].__class__.__name__ == "ImageAttachment":
                        args["message_object"] = a
                        self.loop.create_task(self.standard_2020(["img"], args))
                        return True
                except:
                    pass
                await self.send(Message("Wiadomość jest bez zdjęcia"))
                return True
            else:
                t = round(time.time()) + 120
                if (args["thread_id"] in self.img_query) == False:
                    self.img_query[args["thread_id"]] = {}
                self.img_query[args["thread_id"]][args["author_id"]] = [self.standard_2020, t, None]
                await self.send(Message("Wyślij zdjęcie"), args["thread_id"], args["thread_type"])
            return True
        elif len(command) > 1:
            if command[1] == "@me":
                self.loop.create_task(self.standard_2020(["img"], args, args["author_id"]))
                return True
            else:
                if len(args["message_object"].mentions) > 0:
                    self.loop.create_task(self.standard_2020(["img"], args, args["message_object"].mentions[0].thread_id))
                    return True
                
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"2020 (@me lub oznaczenie)\n1980: In future we will have flying cars\n 2020:"), args["thread_id"], args["thread_type"])
        
    async def standard_wypierdalaj(self, command, args, query_args=None):
        if len(command) == 1:
            if command[0] == "img":
                # try:
                if query_args == None:
                    url = await self.fetch_image_url(args["message_object"].attachments[0].uid)
                else:
                    url = "https://graph.facebook.com/v3.1/"+str(query_args)+"/picture?height=500"
                # except:
                    # j = check_request(self._get(ReqUrl.ATTACHMENT_PHOTO, query={"photo_id": str(args["message_object"].attachments[0].uid)}))["redirect"]
                    # j = self._session.get(j, headers=self._header, verify=self.ssl_verify, allow_redirects=True)
                    # b = str(j.text)
                    # print(b)
                    # url = b[b.find("[\"http")+2:b.find("dl=1\"")+4].replace("\/", "/")
                # print("\n"+str(url)+"\n")
                img = requests.get(url).content
                fn = config.attachments_location+args["message_object"].uid+'_edit.jpg'
                open(fn, 'wb').write(img)

                #edit
                img = Image.open(fn)
                new = Image.new("RGB", img.size)
                wyp = Image.open("saved/templates/wyp.jpg")
                
                h = round(img.height/8)
                
                wyp = wyp.resize((img.width, h))
                new.paste(wyp, (0, img.height-h))
                img = img.resize((img.width, img.height-h))
                new.paste(img, (0, 0))
                
                new.save(fn, quality=97)

                await self.send_local_files([fn], None, args["thread_id"], args["thread_type"])
                os.remove(fn)
            elif args["message_object"].reply_to_id != None:
                try:
                    a = await self.fetch_message_info(args["message_object"].reply_to_id, args["thread_id"])
                    if a.attachments[0].__class__.__name__ == "ImageAttachment":
                        args["message_object"] = a
                        self.loop.create_task(self.standard_wypierdalaj(["img"], args))
                        return True
                except:
                    pass
                await self.send(Message("Wiadomość jest bez zdjęcia"))
                return True
            else:
                t = round(time.time()) + 120
                if (args["thread_id"] in self.img_query) == False:
                    self.img_query[args["thread_id"]] = {}
                self.img_query[args["thread_id"]][args["author_id"]] = [self.standard_wypierdalaj, t, None]
                await self.send(Message("Wyślij zdjęcie"), args["thread_id"], args["thread_type"])
            return True
        elif len(command) > 1:
            if command[1] == "@me":
                self.loop.create_task(self.standard_wypierdalaj(["img"], args, args["author_id"]))
                return True
            else:
                if len(args["message_object"].mentions) > 0:
                    self.loop.create_task(self.standard_wypierdalaj(["img"], args, args["message_object"].mentions[0].thread_id))
                    return True
                
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"wypierdalaj (@me lub oznaczenie)\nDodaje do zdjęcia WYPIERDALAJ."), args["thread_id"], args["thread_type"])

    async def special_vap(self, command, args):
        if "@pedaly" in args["message_object"].text.lower():
            if await self.check_perm("pedaly", args["author_id"], args["thread_id"]):
                mentions = []
                for i in ["100014149658548", "100001661563504", "100013146553559", "100007813848151", "100004858688136", "100009433503633", "100013623423988", "100025253714747", "100002417988928"]:
                    mentions.append(Mention(i, 0, 7))
                await self.send(Message("@pedaly", mentions), args["thread_id"], args["thread_type"])

    async def special_everyone(self, command, args):
        if "@everyone" in args["message_object"].text.lower():
            if await self.check_perm("everyone", args["author_id"], args["thread_id"]):
                await self.send(Message("@everyone", await self.mentions(args["thread_id"])), args["thread_id"], args["thread_type"])
            else:
                await self.send(Message("Nie masz permisji do @everyone"), args["thread_id"], args["thread_type"])

    async def mentions(self, thread_id):
        thread = await self.fetch_thread_info(thread_id)
        thread = list(thread[thread_id].participants)
        mention = []
        for participant in thread:
            mention.append(Mention(participant, 0, 9))
        return mention

    async def special_thinking(self, command, args):
        if args["message_object"].text == "🤔":
            await self.send(Message("🤔"), args["thread_id"], args["thread_type"])

    async def special_grek(self, command, args):
        if args["message_object"].text.lower() == "grek":
            if args["message_object"].text == "Grek":
                await self.send(Message("grek*"), args["thread_id"], args["thread_type"])
            await self.send(Message("to pedał"), args["thread_id"], args["thread_type"])
        if args["message_object"].text.lower() == "pedał":
            await self.send(Message("sam jesteś grek"), args["thread_id"], args["thread_type"])
        if args["message_object"].text.lower() == "pedał to":
            await self.send(Message("grek"), args["thread_id"], args["thread_type"])

    async def special_leet(self, command, args):
        if "1337" in args["message_object"].text:
            if await self.check_perm("leet", args["author_id"], args["thread_id"]):
                await self.send(Message("Jesteś Elitą"), args["thread_id"], args["thread_type"])
            else:
                await self.send(Message("Nie jesteś Elitą"), args["thread_id"], args["thread_type"])

    async def special_pap(self, command, args):
        if "2137" in args["message_object"].text:
            await self.send(Message("haha toż to papieżowa liczba"), args["thread_id"], args["thread_type"])

    # todo: special_spier and special_wyp alias for one function
    async def special_spier(self, command, args):
        if "spierdalaj" == args["message_object"].text.lower():
                await self.send(Message("sam spierdalaj"), args["thread_id"], args["thread_type"])
                await self.react_to_message(args["mid"], MessageReaction.ANGRY)
        elif args["message_object"].text.lower()[0:3] == "sam" and args["message_object"].text.lower().endswith("spierdalaj"):
            t = args["message_object"].text.lower().replace("sam", "")
            t = t.replace(" ", "")
            t = t.replace("spierdalaj", "")
            if t == "" and args["message_object"].text.lower().count("spierdalaj") == 1:
                message = "sam "
                for i in range(args["message_object"].text.lower().count("sam")):
                    message += "sam "
                message += "spierdalaj"
                await self.send(Message(message), args["thread_id"], args["thread_type"])
                await self.react_to_message(args["mid"], MessageReaction.ANGRY)

    async def special_wyp(self, command, args):
        if "wypierdalaj" == args["message_object"].text.lower():
                await self.send(Message("sam wypierdalaj"), args["thread_id"], args["thread_type"])
                await self.react_to_message(args["mid"], MessageReaction.ANGRY)
        elif args["message_object"].text.lower()[0:3] == "sam" and args["message_object"].text.lower().endswith("wypierdalaj"):
            t = args["message_object"].text.lower().replace("sam", "")
            t = t.replace(" ", "")
            t = t.replace("wypierdalaj", "")
            if t == "" and args["message_object"].text.lower().count("wypierdalaj") == 1:
                message = "sam "
                for i in range(args["message_object"].text.lower().count("sam")):
                    message += "sam "
                message += "wypierdalaj"
                await self.send(Message(message), args["thread_id"], args["thread_type"])
                await self.react_to_message(args["mid"], MessageReaction.ANGRY)

    async def special_Xd(self, command, args):
        if "Xd" in args["message_object"].text:
            await self.react_to_message(args["mid"], MessageReaction.ANGRY)

    async def standard_donate(self, command, args):
        if len(command) == 1:
            await self.send(Message('Jakby ktoś chciał wspomóc twórce\nhttps://paypal.me/kugo12'), args["thread_id"], args["thread_type"])
            return True
        elif command == []:
            await self.send(Message('Grosza daj wiedźminowi\nSakiewką potrząśnij\nSakiewką potrząśnij\nłoooooo'), args["thread_id"], args["thread_type"])
            return True

        await self.send(Message("Użycie:\n"+config.cmd_prefix+"donate \nDaje link do peepee twórcy."), args["thread_id"], args["thread_type"])
                
    async def standard_sugestia(self, command, args):
        if len(command) > 2:
            txt = ' '.join(command[1:])
            name = await self.fetch_user_info(args['author_id'])
            name = name[args['author_id']].name

            data = {
                'api': config.wb_site['api_key'],
                'text': txt,
                'name': name
            }

            response = requests.post(url=config.wb_site['add_suggestion_url'], data=data)

            if response.status_code == 200:
                try:
                    a = json.loads(response.text)
                    await self.send(Message(f'Sugestia pomyślnie dodana, znajduje się pod adresem:\nhttps://{a["url"]}'), args["thread_id"], args["thread_type"])
                    return True
                except:
                    pass
            await self.send(Message('Coś poszło nie tak'), args["thread_id"], args["thread_type"])

            return True

        await self.send(Message("Użycie:\n"+config.cmd_prefix+"sugestia (tekst, minimum dwa slowa)\nDodaje sugestie do https://wiertarbot.pl/sugestie"), args["thread_id"], args["thread_type"])

    async def standard_changelog(self, command, args):
        if len(command) == 1:
            await self.send(Message("https://wiertarbot.pl/changelog"), args["thread_id"], args["thread_type"])
            return True
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"changelog \nDaje link do spisu zmian."), args["thread_id"], args["thread_type"])
                
    async def standard_szkaluj(self, command, args):
        if len(command) > 0:
            uid = args["author_id"]
            try:
                tmpid = args["message_object"].mentions[0].thread_id
                if not await self.check_perm('nieszkaluj', tmpid, args['thread_id']):
                    uid = tmpid
            except:
                pass
            try:
                if command[1] == "@random":
                    uid = await self.fetch_thread_info(args["thread_id"])
                    uid = random.choice(list(uid[args["thread_id"]].participants))
            except:
                pass
            who = await self.fetch_user_info(uid)
            who = who[uid]
            who = "@" + who.name
            txt = random.choice(open("szkaluj.txt", encoding="utf-8").read().split("\n"))
            txt = txt.replace("%n%", "\n")
            mentions = []
            while "%on%" in txt:
                mentions.append(Mention(uid, txt.find("%on%"), len(who)))
                txt = txt.replace("%on%", who, 1)
            await self.send(Message(txt, mentions), args["thread_id"], args["thread_type"])
            return True
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"szkaluj (oznaczenie kogos/@random)\nSzkaluje"), args["thread_id"], args["thread_type"])

    async def standard_moneta(self, command, args):
        if len(command) == 1:
            rzut = random.randint(0, 1)
            if rzut == 0:
                await self.send(Message("Orzeł!"), args["thread_id"], args["thread_type"])
            else:
                await self.send(Message("Reszka!"), args["thread_id"], args["thread_type"])
            return True
        
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"moneta\nRzuca monetą."), args["thread_id"], args["thread_type"])

    async def standard_kostka(self, command, args):
        if len(command) == 1:
            rzut = random.randint(1, 6)
            await self.send(Message("Wyrzuciłeś "+str(rzut)), args["thread_id"], args["thread_type"])
            return True
        
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"kostka\nRzuca kostką"), args["thread_id"], args["thread_type"])

    async def mcd(self, typ, args):
        r = ['118', '121', '129', '145', '148', '151', '154', '157', '161', '170', '171', '173', '176', '181', '191', '199', '201', '215', '216', '220', '232', '236', '246', '249', '252', '253', '256', '257', '276', '322', '335', '337', '338', '349', '359', '362', '364', '375', '380', '384', '390', '396', '400', '421', '431', '435', '437', '438', '448', '450', '452', '454', '455', '468', '480', '481', '483', '484', '485', '487', '489', '490', '491', '492', '493', '494', '495', '496', '497', '498', '499']
        t = time.time()-60
        d = datetime.datetime.fromtimestamp(t)

        def s(a):
            return ("0" + str(a))[-2:]
        def g():
            while True:
                re = str(random.randint(135, 535))
                if re in r:
                    pass
                else:
                    return re
        
        kod = int(''.join([g(), s(d.month), s(d.day), s(d.hour), s(d.minute), '10', '05', str(random.randint(100, 999))]), 10)
        kod = baseconvert.base(kod, 10, 32, string=True)[0:11]
        if typ == "hamburger":
            image = Image.open("saved/kupony/kuponhamburger.jpg")
        else:
            image = Image.open("saved/kupony/kuponlody.jpg")

        draw = ImageDraw.Draw(image)
        font = ImageFont.truetype("arialbd.ttf", 16)
        draw.text((3, 55), "DATA WYDANIA "+time.strftime("%d-%m-%Y", time.gmtime(round(time.time())-86400)), font=font, fill="#000")
        # draw.text((3, 5), "Dziękujemy za udział w badaniu. Przesyłamy wybrany przez Pana/ią kupon.", font=font, fill="#000")
        tekst = "UNIKALNY KOD: 0"+kod
        size = draw.textsize(tekst, font=font)
        draw.text((577-size[0], 55), tekst, font=font, fill="#000")
        image.save("saved/kupony/"+args["mid"]+".jpg", "JPEG", quality=100)

        await self.send_local_files(["saved/kupony/"+args["mid"]+".jpg"], None, args["thread_id"], args["thread_type"])

        os.remove("saved/kupony/"+args["mid"]+".jpg")
        return True

    async def standard_mcd(self, command, args):
        if len(command) == 2:
            if command[1] in ["hamburger", "lody"]:
                self.loop.create_task(self.mcd(command[1], args))
                return True
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"mcd <hamburger/lody>\nWysyła kupon z ankiety McDonald's"), args["thread_id"], args["thread_type"])

#    async def download(self, link, args):
#        vmax = 25000000
#        try:
#            yt = YouTube(link)
#        except KeyError:
#            try:
#                yt = YouTube(link)
#            except KeyError:
#                yt = YouTube(link)
#        streams = yt.streams.filter(progressive=True, mime_type="video/mp4").all()
#        for stream in streams:
#            if stream.filesize < vmax:
#                fn = args["mid"].replace(".", "").replace("$", "")+".mp4"
#                stream.download(filename=fn)
#                await self.send_local_files([fn], None, args["thread_id"], args["thread_type"])
#                os.remove(fn)
#                return True
#        await self.send(Message("Za długie wideo lub brak streamów do 25MB"), args["thread_id"], args["thread_type"])
#        return True
#
#    async def standard_download(self, command, args):
#        if len(command) == 2:
#            if "youtu" in command[1]:
#
#                self.loop.create_task(self.download(args["message_object"].text.split(" ")[1], args))
#                return True
#            else:
#                await self.send(Message("Nieprawidłowy link"), args["thread_id"], args["thread_type"])
#                return True
#        await self.send(Message("Użycie:\n"+config.cmd_prefix+"download <link do youtube>\nWysyła film z yt"), args["thread_id"], args["thread_type"])

    async def tts(self, msg, lang, args):
        s = gTTS(text=msg, lang=lang)
        fn = "saved/tts/"+args["mid"]+".mp3"
        await asyncio.sleep(0)
        s.save("saved/tts/"+args["mid"]+".mp3")
        await self.send_local_files([fn], None, args["thread_id"], args["thread_type"])
        return True

    async def standard_tts(self, command, args):
        if len(command) > 1:
            msg = args["message_object"].text[5:].lower()
            lang = "pl"
            if command[1].startswith("lang="):
                if len(command) == 2:
                    return await self.standard_tts([], {})

                lang = command[1].replace("lang=", "")
                msg = msg.replace(command[1], "")[1:]
            self.loop.create_task(self.tts(msg, lang, args))
            return True
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"tts (opcjonalnie język tekstu, np. lang=en) <tekst>\nWysyła nagranie z tekstem (tts - text to speech)\nDomyślny język - pl\nKody językowe: wiertarbot.pl/tts"), args["thread_id"], args["thread_type"])
        return False

    async def standard_mc(self, command, args):
        if len(command) == 3:
            nick = command[2]
            if command[1] == "names":
                uuid = requests.get("https://api.mojang.com/users/profiles/minecraft/"+nick).text
                if uuid == "":
                    await self.send(Message("Podany nick nie istnieje"), args["thread_id"], args["thread_type"])
                    return True
                uuid = json.loads(uuid)["id"]

                names = json.loads(requests.get("https://api.mojang.com/user/profiles/"+uuid+"/names").text)
                text = "Oryginalny: "+names[0]["name"]+"\n"
                for name in names[1:]:
                    date = datetime.datetime.fromtimestamp(int(str(name["changedToAt"])[:-3]))
                    text += str(date)+": "+name["name"]+"\n"
                await self.send(Message(text), args["thread_id"], args["thread_type"])
                return True
            elif command[1] == "skin":
                uuid = requests.get("https://api.mojang.com/users/profiles/minecraft/"+nick).text
                if uuid == "":
                    await self.send(Message("Podany nick nie istnieje"), args["thread_id"], args["thread_type"])
                    return True
                uuid = json.loads(uuid)["id"]

                skin = ["https://crafatar.com/skins/"+uuid+".png?default=MHF_Steve", 
                        "https://crafatar.com/renders/body/"+uuid+".png?default=MHF_Steve&overlay=True&scale=2", 
                        "https://crafatar.com/avatars/"+uuid+".png?default=MHF_Steve", 
                        "https://crafatar.com/renders/head/"+uuid+".png?default=MHF_Steve&overlay=True&size=64"
                        ]
                await self.send_remote_files(skin, None, args["thread_id"], args["thread_type"])
                return True
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"mc <names/skin> <nick>\nnames - wysyła historie nazw\nskin - wysyła skin"), args["thread_id"], args["thread_type"])

    # async def standard_mb(self, command, args):
    #     if len(command) == 2:
    #         if command[1] == "numerek":
    #             await self.send(Message("Szczęśliwy numerek na dziś: "+self.mb.get_lucky_number()), args["thread_id"], args["thread_type"])
    #             return True
    #         elif command[1] == "dyzurni":
    #             a = self.mb.get_duty()
    #             b = ""
    #             for c in a:
    #                 b = b+"\n"+c["who"]
    #             await self.send(Message("Od "+a[0]["start"]+" do "+a[0]["end"]+" dyżurnymi są:"+b), args["thread_id"], args["thread_type"])
    #     else:
    #         await self.send(Message("Użycie:\n"+config.cmd_prefix+"mb <numerek/dyzurni>"), args["thread_id"], args["thread_type"])  

    # async def standard_ip(self, command, args):
    #     if len(command) == 1:
    #         if args["thread_type"] == ThreadType.USER:
    #             await self.send(Message(get("https://api.ipify.org").text), args["thread_id"], args["thread_type"])
    #         else:
    #             await self.send(Message("nic"), args["thread_id"], args["thread_type"])
    #     if command == []:
    #         await self.send(Message("Użycie:\n"+config.cmd_prefix+"ip\nWysyła publiczne ip hosta"), args["thread_id"], args["thread_type"])

    async def standard_bc(self, command, args):
        if len(command) > 1:
            a = args["message_object"].text.replace("!bc ", "")
            b = await self.fetch_thread_list()
            for c in b:
                await self.send(Message(a), c.uid, c.type)
            return True
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"bc <wiadomosc>\nRozgłasza wiadomość w wątkach w których uczestniczy bot."), args["thread_id"], args["thread_type"])

    async def standard_tencza(self, command, args):
        if len(command) == 1:
            for i in range(10):
                await self.change_thread_color(random.choice(colors), args["thread_id"])
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"tencza\nKolorkiiiiii"), args["thread_id"], args["thread_type"])

    async def standard_track(self, command, args):
        if len(command) == 2:
            ap = AliPaczka(command[1])
            await self.send(Message(str(ap)), args['thread_id'], args['thread_type'])
        else:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"track <numer sledzenia>\nPokazuje statusy paczki."), args["thread_id"], args["thread_type"])

    async def standard_pandka(self, command, args):
        if len(command) == 1:
            pandka = [json.loads(requests.get("https://some-random-api.ml/img/red_panda").text)["link"]]
            await self.send_remote_files(pandka, None, args["thread_id"], args["thread_type"])
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"pandka\nWysyła losowe zdjęcie pandki"), args["thread_id"], args["thread_type"])

    async def standard_konon(self, command, args):
        if len(command) == 1:
            await self.send_local_files(["konon/" + random.choice(os.listdir("konon"))], None, args["thread_id"], args["thread_type"])
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"konon\nWysyła losowe zdjęcie Kononowicza"), args["thread_id"], args["thread_type"])

    async def standard_papaj(self, command, args):
        if len(command) == 1:
            await self.send_local_files(["cenzo/" + random.choice(os.listdir("cenzo"))], None, args["thread_id"], args["thread_type"])
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"papaj\nWysyła losowy mem z papieżem"), args["thread_id"], args["thread_type"])

    async def standard_bmw(self, command, args):
        if len(command) == 1:
            await self.send_local_files(["bmw/" + random.choice(os.listdir("bmw"))], None, args["thread_id"], args["thread_type"])
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"bmw\nWysyła losowe zdjęcie zezłomowanego bmw"), args["thread_id"], args["thread_type"])

    async def standard_mikser(self, command, args):
        if len(command) == 1:
            await self.send_local_files(["mikser/" + random.choice(os.listdir("mikser"))], None, args["thread_id"], args["thread_type"])
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"mikser\nWysyła losowe zdjęcie miksera"), args["thread_id"], args["thread_type"])

    async def standard_shiba(self, command, args):
        if len(command) in [1, 2]:
            a = "1"
            if len(command) == 2:
                try:
                    if 0 < int(command[1]) < 11:
                        if await self.check_perm("shibamore", args["author_id"], args["thread_id"]):
                            a = command[1]
                except:
                    pass

            response = requests.get("https://shibe.online/api/shibes?count="+a+"&urls=true&httpsUrls=true")
            shiba = json.loads(response.text)
            await self.send_remote_files(shiba, None, args["thread_id"], args["thread_type"])
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"shiba\nWysyła losowe zdjęcie shiby"), args["thread_id"], args["thread_type"])

    async def standard_birb(self, command, args):
        if len(command) == 1:
            birb = [json.loads(requests.get("https://some-random-api.ml/img/birb").text)["link"]]
            await self.send_remote_files(birb, None, args["thread_id"], args["thread_type"])
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"birb\nWysyła losowe zdjęcie ptaka (xD)"), args["thread_id"], args["thread_type"])
    
    async def standard_pikachu(self, command, args):
        if len(command) == 1:
            pikachu = [json.loads(requests.get("https://some-random-api.ml/pikachuimg").text)["link"]]
            await self.send_remote_files(pikachu, None, args["thread_id"], args["thread_type"])
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"birb\nWysyła losowe zdjęcie ptaka (xD)"), args["thread_id"], args["thread_type"])

    async def standard_catto(self, command, args):
        if len(command) == 1:
            response = requests.get("https://api.thecatapi.com/v1/images/search", config.thecatapi_headers)
            cat = json.loads(response.text)
            await self.send_remote_files([cat[0]["url"]], None, args["thread_id"], args["thread_type"])
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"catto\nWysyła losowe zdjęcie kota"), args["thread_id"], args["thread_type"])

    async def standard_doggo(self, command, args):
        if len(command) == 1:
            response = requests.get("https://dog.ceo/api/breeds/image/random")
            dog = json.loads(response.text)
            await self.send_remote_files([dog["message"]], None, args["thread_id"], args["thread_type"])
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"doggo\nWysyła losowe zdjęcie psa"), args["thread_id"], args["thread_type"])

    async def standard_beagle(self, command, args):
        if len(command) == 1:
            response = requests.get("https://dog.ceo/api/breed/beagle/images/random")
            beagle = json.loads(response.text)
            await self.send_remote_files([beagle["message"]], None, args["thread_id"], args["thread_type"])
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"beagle\nWysyła losowe zdjęcie beagle'a"), args["thread_id"], args["thread_type"])

    async def standard_jez(self, command, args):
        if len(command) == 1:
            response = requests.get("http://www.cutestpaw.com/tag/hedgehogs/page/"+str(random.randint(1, 10))+"/")
            h = BeautifulSoup(response.text, "html.parser")
            h = h.find_all("a", {"title": True})
            await self.send_remote_files([random.choice(h).img["src"]], None, args["thread_id"], args["thread_type"])
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"jez\nWysyła losowe zdjęcie jeża"), args["thread_id"], args["thread_type"])

    async def standard_zolw(self, command, args):
        if len(command) == 1:
            response = requests.get("http://www.cutestpaw.com/tag/tortoises/page/"+str(random.randint(1, 8))+"/")
            h = BeautifulSoup(response.text, "html.parser")
            h = h.find_all("a", {"title": True})
            await self.send_remote_files([random.choice(h).img["src"]], None, args["thread_id"], args["thread_type"])
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"zolw\nWysyła losowe zdjęcie żółwia"), args["thread_id"], args["thread_type"])

    async def standard_suchar(self, command, args):
        if len(command) == 1:
            response = requests.get("http://www.suchary.com/random.html")
            if response.status_code == 404:
                await self.send(Message("Błąd"), args["thread_id"], args["thread_type"])
            else:
                parsed = BeautifulSoup(response.text, "html.parser")
                suchar = parsed.body.find("div", "file-container").a.img["src"]
                await self.send_remote_files([suchar], None, args["thread_id"], args["thread_type"])
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"suchar\nWyświetla losowy suchar"), args["thread_id"], args["thread_type"])

    async def standard_miejski(self, command, args):
        if len(command) > 1:
            word = args["message_object"].text.lower().replace("!miejski ", "")
            response = requests.get("https://www.miejski.pl/slowo-" + word.replace(" ", "+"))
            if response.status_code == 404:
                response = None
                await self.send(Message("Nie znaleziono takiego słowa"), args["thread_id"], args["thread_type"])
            else:
                parsed = BeautifulSoup(response.text, "html.parser")
                m = parsed.body.find("main")
                definition = m.find('p').get_text()
                try:
                    example = m.find('blockquote').get_text()
                    example = "\n\nPrzyklad/y:" + example
                except:
                    example = ''
                message = word + "\nDefinicja:" + definition + example
                await self.send(Message(message), args["thread_id"], args["thread_type"])
            return True
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"miejski <słowo/zwrot>\nWyświetla definicję słowa/zwrotu z miejski.pl"), args["thread_id"], args["thread_type"])

    async def standard_mem(self, command, args):
        if len(command) == 1:
            rand = random.choice(["memy", "demotywatory", "jeja"])
            if rand == "memy":
                response = requests.get("http://memy.pl/losuj")
                if response.status_code == 404:
                    await self.send(Message("Błąd"), args["thread_id"], args["thread_type"])
                else:
                    parsed = BeautifulSoup(response.text, "html.parser")
                    img = parsed.body.find("a", "img-responsive").find("img")["src"]
                    await self.send_remote_files([img], None, args["thread_id"], args["thread_type"])
            elif rand == "demotywatory":
                response = requests.get("https://demotywatory.pl/losuj")
                if response.status_code == 404:
                    await self.send(Message("Błąd"), args["thread_id"], args["thread_type"])
                else:
                    parsed = BeautifulSoup(response.text, "html.parser")
                    img = parsed.body.find("img", "demot")
                    await self.send_remote_files([img["src"]], None, args["thread_id"], args["thread_type"])
            elif rand == "jeja":
                response = requests.get("https://memy.jeja.pl/losowe")
                if response.status_code == 404:
                    await self.send(Message("Błąd"), args["thread_id"], args["thread_type"])
                else:
                    parsed = BeautifulSoup(response.text, "html.parser")
                    img = parsed.body.find("img", ["ob-left-image", "ob-image-j"])["src"]
                    await self.send_remote_files([img], None, args["thread_id"], args["thread_type"])
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"mem\nWysyła losowy mem z losowego źródła (jeja, demotywatory, memy.pl)"), args["thread_id"], args["thread_type"])

    async def standard_kod(self, command, args):
        if len(command) == 1:
            await self.send(Message("https://github.com/kugo12/WiertarBot"), args["thread_id"], args["thread_type"])
            return True
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"kod\nnie dla psa"), args["thread_id"], args["thread_type"])  

    async def standard_barka(self, command, args):
        if len(command) == 1:
            await self.send(Message("Pan kiedyś stanął nad brzegiem\nSzukał ludzi gotowych pójść za Nim\nBy łowić serca\nSłów Bożych prawdą.\n\nRef.:\nO Panie, to Ty na mnie spojrzałeś,\nTwoje usta dziś wyrzekły me imię.\nSwoją barkę pozostawiam na brzegu,\nRazem z Tobą nowy zacznę dziś łów.\n\n2.\nJestem ubogim człowiekiem,\nMoim skarbem są ręce gotowe\nDo pracy z Tobą\nI czyste serce.\n\n3.\nTy, potrzebujesz mych dłoni,\nMego serca młodego zapałem\nMych kropli potu\nI samotności.\n\n4.\nDziś wypłyniemy już razem\nŁowić serca na morzach dusz ludzkich\nTwej prawdy siecią\nI słowem życia.\n\n\nBy Papież - https://www.youtube.com/watch?v=fimrULqiExA\nZ tekstem - https://www.youtube.com/watch?v=_o9mZ_DVTKA"), args["thread_id"], args["thread_type"])
            return True
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"barka\nWyświetla tekst barki"), args["thread_id"], args["thread_type"])

    async def standard_xd(self, command, args):
        if len(command) == 1:
            if args["message_object"].text == config.cmd_prefix+"Xd":
                await self.send(Message("Serio, mało rzeczy mnie triggeruje tak jak to chore \"Xd\". Kombinacji x i d można używać na wiele wspaniałych sposobów. Coś cię śmieszy? Stawiasz \"xD\". Coś się bardzo śmieszy? Śmiało: \"XD\"! Coś doprowadza Cię do płaczu ze śmiechu? \"XDDD\" i załatwione. Uśmiechniesz się pod nosem? \"xd\". Po kłopocie. A co ma do tego ten bękart klawiaturowej ewolucji, potwór i zakała ludzkiej estetyki - \"Xd\"? Co to w ogóle ma wyrażać? Martwego człowieka z wywalonym jęzorem? Powiem Ci, co to znaczy. To znaczy, że masz w telefonie włączone zaczynanie zdań dużą literą, ale szkoda Ci klikać capsa na jedno \"d\" później. Korona z głowy spadnie? Nie sondze. \"Xd\" to symptom tego, że masz mnie, jako rozmówcę, gdzieś, bo Ci się nawet kliknąć nie chce, żeby mi wysłać poprawny emotikon. Szanujesz mnie? Używaj \"xd\", \"xD\", \"XD\", do wyboru. Nie szanujesz mnie? Okaż to. Wystarczy, że wstawisz to zjebane \"Xd\" w choć jednej wiadomości. Nie pozdrawiam"), args["thread_id"], args["thread_type"])
                return True
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"Xd\nWyświetla znaną pastę o \"Xd\""), args["thread_id"], args["thread_type"])

    # todo: remove @me
    async def standard_prof(self, command, args):
        if len(command) > 1:
            if command[1] == "@me":
                url = "https://graph.facebook.com/v3.1/"+str(args["author_id"])+"/picture?height=500"
                await self.send_remote_files(file_urls=[url], thread_id=args["thread_id"], thread_type=args["thread_type"])
                return True
            elif len(args["message_object"].mentions) > 0:
                url = "https://graph.facebook.com/v3.1/"+str(args["message_object"].mentions[0].thread_id)+"/picture?height=500"
                await self.send_remote_files(file_urls=[url], thread_id=args["thread_id"], thread_type=args["thread_type"])
                return True
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"prof <@me lub oznaczenie>\nWysyła zdjęcie profilowe"), args["thread_id"], args["thread_type"])

    async def standard_wyborynastaroste(self, command, args):
        await self.send(Message("Starostą zostaje... Marcin Salamandra!", mentions=[Mention("100001193386500", 20, 17)]), thread_id=args["thread_id"], thread_type=args["thread_type"])

    async def standard_covid(self, command, args):
        if len(command) == 1:
            add = "http://35.205.149.154:5000/api"
            a = requests.get(add).text
            a = json.loads(a)
            n = a['new']
            ni = " ("+str(n['infected'])+" nowych)" if n['infected'] else ""
            nd = " ("+str(n['deaths'])+" nowych)" if n['deaths'] else ""
            # nr = " ("+str(n['recovered'])+" nowych)" if n['recovered'] else ""
            msg = "Statystyki COVID19 w Polsce na ten moment:\n"+str(a['infected'])+" chorych"+ni+"\n"
            msg += str(a['deaths'])+" śmierci"+nd+"\n"
            # msg += str(a['recovered'])+" wyleczonych"+nr+"\n"
            msg += "szczegolowe dane !covid s\npoki wyleczonych nie ma na gov.pl to tutaj tez nie"
            await self.send(Message(msg), args["thread_id"], args["thread_type"])
            return True 
        elif len(command) == 2:
            if command[1] == "s":
                add = "http://35.205.149.154:5000/api/detailed"
                a = requests.get(add).text
                a = json.loads(a)
                msg = ""
                for i in a:
                    n = a[i]['new']
                    ni = " ("+str(n['infected'])+" nowych)" if n['infected'] else ""
                    nd = " ("+str(n['deaths'])+" nowych)" if n['deaths'] else ""
                    # nr = " ("+str(n['recovered'])+" nowych)" if n['recovered'] else ""
                    msg += i+":\n"
                    msg += " "+str(a[i]['infected'])+" chorych"+ni+"\n"
                    msg += " "+str(a[i]['deaths'])+" śmierci"+nd+"\n"
                    # msg += " "+str(a[i]['recovered'])+" wyleczonych"+nr+"\n"
                await self.send(Message(msg), args["thread_id"], args["thread_type"])
                return True

        await self.send(Message("Użycie:\n"+config.cmd_prefix+"covid (s)\nAktualne dane z gov.pl"), args["thread_id"], args["thread_type"])

    async def standard_nns(self, command, args):
        if len(command) > 1:
            nickname = ' '.join(args["message_object"].text.split(" ")[1:])
            uids = []
            if args["thread_type"] == ThreadType.GROUP:
                uids = await self.fetch_group_info(args["thread_id"])[args["thread_id"]].participants
            elif args["thread_type"] == ThreadType.USER:
                uids = [self.uid, args["thread_id"]]
            for uid in uids:
                await self.change_nickname(nickname, uid, args["thread_id"], args["thread_type"])
            return True
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"nns <nazwa>\nZmienia nazwy każdemu w wątku"), args["thread_id"], args["thread_type"])

    # todo: fix this
    async def standard_uptime(self, command, args):
        if len(command) == 1:
            ut = round(time.time()) - self.uptime
            txt = "Czas działania bota: " + str(int((ut - ut % 86400) / 86400)) + "dni " + time.strftime("%Hh %Mmin %Ssek", time.gmtime(int(ut)))
            ut = await self.server_uptime()
            txt = txt+"\nCzas działania serwera: " + str(int((ut - ut % 86400) / 86400)) + "dni " + time.strftime("%Hh %Mmin %Ssek", time.gmtime(int(ut)))
            await self.send(Message(txt), args["thread_id"], args["thread_type"])
            return True
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"uptime\nPokazuje czas działania bota od ostatniego uruchomienia"), args["thread_id"], args["thread_type"])

    async def standard_czas(self, command, args):
        now = datetime.datetime.now()
        tera = now.strftime("%A %d %B %H:%M")
        repl = {"January": "Stycznia", "February": "Lutego", "March": "Marca", "April": "Kwietnia", "May": "Maja", "June": "Czerwca", "July": "Lipca", "August": "Sierpnia", "September": "Września", "October": "Października", "November": "Listopada", "December": "Grudnia", "Monday": "Poniedziałek", "Tuesday": "Wtorek", "Wednesday": "Środa", "Thursday": "Czwartek", "Friday": "Piątek", "Saturday": "Sobota", "Sunday": "Niedziela"}
        for i in repl:
            tera = tera.replace(i, repl[i])

        # todo: add timezone support
        czas = round(time.time())
        wiadomosc = "Jest: " + tera
        if len(command) == 1:
            czasdowakacji = 1593118800 - czas
            if czasdowakacji > 0:
                wiadomosc = wiadomosc + "\nPoczątek wakacji (26 czerwca) za: " + str(int((czasdowakacji - czasdowakacji % 86400) / 86400)) + "dni " + time.strftime("%Hh %Mmin %Ssek", time.gmtime(int(round(czasdowakacji))))

            await self.send(Message(wiadomosc), args["thread_id"], args["thread_type"])
            return True
        if len(command) == 2:
            pass

        await self.send(Message("Użycie:\n"+config.cmd_prefix+"czas\nWyświetla czas"), args["thread_id"], args["thread_type"])

    async def standard_ile(self, command, args):
        if len(command) == 1:
            ile = await self.fetch_thread_info(args["thread_id"])
            await self.send(Message("Odkąd mnie dodano napisano "+str(ile[args["thread_id"]].message_count)+" wiadomości"), args["thread_id"], args["thread_type"])
            return True
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"ile\nWyświetla ile wiadomości zostało napisane od momentu dodania bota"), args["thread_id"], args["thread_type"])

    async def standard_zadanie(self, command, args):
        await self.standard_n(command, args)

    async def standard_n(self, command, args):
        if len(command) > 3:
            tid = args["thread_id"]
            if await self.check_perm("ntid", args["author_id"], args["thread_id"]):
                if command[3][:4] == "tid=":
                    try:
                        tid = int(command[3][4:])
                    except:
                        pass
            if command[1] == "add":
                if await self.check_perm("nadd", args["author_id"], args["thread_id"]):
                    t = round(time.time())
                    n = 604800 + t
                    if command[2][-1:] == "h":
                        try:
                            n = int(command[2])*3600 + t
                        except:
                            pass
                    if command[2] == "*":
                        n = "*"
                    try:
                        command[2] = command[2].replace("r.", "")
                        command[2] = command[2].replace("r", "")
                        n = int(time.mktime(time.strptime(command[2], "%d.%m.%Y"))) + 86400
                    except:
                        pass
                    
                    note = ' '.join(args["message_object"].text.split(" ")[3:])

                    self.cur.execute("INSERT INTO notes (mid, uid, tid, start, end, note) VALUES (?, ?, ?, ?, ?, ?)", [args["mid"], args["author_id"], tid, t, n, note])
                    self.conn.commit()
                    await self.send(Message("Zapisano"), args["thread_id"], args["thread_type"])
                return True
        if 3 <= len(command) <= 4:
            if command[1] == "rem":
                if await self.check_perm("nrem", args["author_id"], args["thread_id"]):
                    if len(command) == 4:
                        if await self.check_perm("ntid", args["author_id"], args["thread_id"]):
                            if command[3][:4] == "tid=":
                                try:
                                    tid = int(command[3][4:])
                                except:
                                    pass

                    try:
                        nid = int(command[2])
                    except ValueError:
                        await self.send(Message("Nieprawidłowe ID"), args["thread_id"], args["thread_type"])
                        return True

                    self.cur.execute("SELECT * FROM notes WHERE id = ?", [nid])
                    note = self.cur.fetchone()
                    
                    self.cur.execute("INSERT INTO deletedNotes (id, mid, uid, tid, start, end, note) VALUES (?, ?, ?, ?, ?, ?, ?)", [nid, note[1], note[2], note[3], note[4], note[5], note[6]])
                    self.cur.execute("DELETE FROM notes WHERE id = ?", [nid])
                    self.conn.commit()

                    await self.send(Message("Usunięto"), args["thread_id"], args["thread_type"])
                return True
        if 1 <= len(command) <= 3:
            more = False
            if len(command) > 1:
                if await self.check_perm("nmore", args["author_id"], args["thread_id"]):
                    more = command[1] == "m"

            tid = args["thread_id"]
            if len(command) > 1:
                if await self.check_perm("ntid", args["author_id"], args["thread_id"]):
                    if command[1][:4] == "tid=":
                        try:
                            tid = int(command[1][4:])
                        except:
                            pass
                    if len(command) > 2:
                        if command[2][:4] == "tid=":
                            try:
                                tid = int(command[2][4:])
                            except:
                                pass
            self.cur.execute("SELECT * FROM notes WHERE tid = ? ORDER BY end ASC", [tid])
            notes = self.cur.fetchall()
            if len(notes) == 0:
                await self.send(Message("Brak notatek"), args["thread_id"], args["thread_type"])
                return True
            
            mess = ""
            for i in notes:
                if more:
                    mess += str(i[0]) + " " + i[2] + " " + i[4] + " " + i[5] +"\n"
                mess += i[6] + "\n"
            
            await self.send(Message(mess), args["thread_id"], args["thread_type"])
            return True
          
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"n (add/rem/m)\n"+config.cmd_prefix+"n add <czas w godzinach lub *> <tekst>\nNotatki"), args["thread_id"], args["thread_type"])

    async def standard_help(self, command, args):
        if command == []:
            await self.send(Message("Użycie:\n"+config.cmd_prefix+"help (komenda)\nPokazuje wszystkie komendy lub jej użycie"), args["thread_id"], args["thread_type"])
            return True
        elif len(command) == 2:
            if "standard_"+command[1] in dir(self):
                await getattr(self, "standard_"+command[1])([], args)
                return True
            await self.send(Message("Podana komenda nie istnieje"), args["thread_id"], args["thread_type"])
        # todo: not hardcoded command list in self.help_text
        await self.send(Message(self.help_text), args["thread_id"], args["thread_type"])

    async def standard_uid(self, command, args):
        if command:
            if args["message_object"].mentions:
                await self.send(Message(args["message_object"].mentions[0].thread_id), args["thread_id"], args["thread_type"])
            else:
                await self.send(Message(args["author_id"]), args["thread_id"], args["thread_type"])
            return True
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"uid (oznaczenie)\nWyświetla ID użytkownika"), args["thread_id"], args["thread_type"])

    async def standard_tid(self, command, args):
        if len(command) == 1:
            await self.send(Message(args["thread_id"]), args["thread_id"], args["thread_type"])
            return True
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"tid\nWyświetla ID wątku (m.in. grupy)"), args["thread_id"], args["thread_type"])

    # todo: alias of standard_perm as standard_ban
    async def standard_ban(self, command, args):
        if len(command) > 1:
            cmd = await self.perm("banned")
            for i in range(len(command)-1):
                try: 
                    int(command[i+1])
                except ValueError:
                    continue
                cmd[0].append(command[i+1])
            mt = args["message_object"].mentions
            for i in mt:
                cmd[0].append(i.thread_id)
            a = [json.dumps(cmd[0]), json.dumps(cmd[1])]

            await self.change_perm("banned", a)
            await self.send(Message("Zbanowano"), args["thread_id"], args["thread_type"])
            return True

        await self.send(Message("Użycie:\n"+config.cmd_prefix+"ban <id/oznaczenie>\nWyłącza możliwość korzystania z bota."), args["thread_id"], args["thread_type"])

    # todo: alias of standard_perm as standard_unban
    async def standard_unban(self, command, args):
        if len(command) > 1:
            cmd = await self.perm("banned")
            for i in range(len(command)-1):
                try: 
                    int(command[i+1])
                except ValueError:
                    continue
                while command[i+1] in cmd[0]: cmd[0].remove(command[i+1])
            mt = args["message_object"].mentions
            for i in mt:
                while i.thread_id in cmd[0]: cmd[0].remove(i.thread_id)

            await self.change_perm("banned", [json.dumps(cmd[0]), json.dumps(cmd[1])])
            await self.send(Message("Odbanowano"), args["thread_id"], args["thread_type"])
            return True

        await self.send(Message("Użycie:\n"+config.cmd_prefix+"unban <id/oznaczenie>\nWłącza możliwość korzystania z bota."), args["thread_id"], args["thread_type"])
 ``
    async def standard_perm(self, command, args):
        if len(command) == 3:
            if command[1] == "look":
                a = await self.perm(command[2])
                if a:
                    await self.send(
                        Message(f'{ command[2] }:\n\nwhitelist: { a[0] }\nblacklist: { a[1] }'),
                        args["thread_id"], args["thread_type"]
                    )
                else:
                    await self.send(
                        Message("Podana permisja nie istnieje w bazie danych"),
                        args["thread_id"], args["thread_type"]
                    )
                return True

        elif len(command) > 4:
            tid = False
            if command[4][:4] == "tid=":
                try:
                    tid = str(int(command[4][4:]))
                except ValueError:
                    if command[4][4:] == "here":
                        tid = args["thread_id"]

            if command[3] == 'wl':
                bl = 0
            elif command[3] == 'bl':
                bl = 1
            else:
                # send help text
                await self.standard_perm([], args)
                return True

            cmd = await self.perm(command[2])
            if command[1] == 'add':
                add = True
            elif command[1] == 'rm':
                add = False
                if cmd is False:
                    await self.send(
                        Message("Podana permisja nie istnieje w bazie danych"),
                        args["thread_id"], args["thread_type"]
                    )
                    return True
            else:
                # send help text
                await self.standard_perm([], args)
                return True

            uids = command[3:]
            for mention in args['message_object'].mentions:
                uids.append(mention.thread_id)

            text = 'dodano do ' if add else 'usunięto z '
            text += 'blacklisty' if bl else 'whitelisty'

            await self.edit_perm(command[2], uids, bl, add, tid)
            await self.send(Message(f'Pomyślnie { text } '), args["thread_id"], args["thread_type"])
            return True

        await self.send(
            Message("Użycie:\n"+config.cmd_prefix+"perm look <nazwa komendy>\n"+config.cmd_prefix+"perm <add/rem> <nazwa komendy> <bl/wl> <oznaczenie/id>\nKomenda do zarządzania permisjami."),
            args["thread_id"], args["thread_type"]
        )

    async def perm(self, command):
        self.cur.execute("SELECT * FROM permissions WHERE command = ?", [command])
        a = self.cur.fetchone()
        if a == None:
            return False
        return [json.loads(a[1]), json.loads(a[2])]

    async def change_perm(self, command, perms):
        if await self.perm(command) == False:
            self.cur.execute("INSERT INTO permissions (command, whitelist, blacklist) VALUES (?, ?, ?)", [command, perms[0], perms[1]])
            self.conn.commit()
            return True
        self.cur.execute("UPDATE permissions SET whitelist = ?, blacklist = ? WHERE command = ?", [perms[0], perms[1], command])
        self.conn.commit()
        return False

    async def edit_perm(self, perm, uids, bl=False, add=True, tid=False):
        cmd = await self.perm(perm)
        if (cmd) or ((not cmd) and add):
            bl = 1 if bl else 0
            if not cmd:
                cmd = [{}, {}]

            for uid in uids:
                try:
                    int(uid)
                except ValueError:
                    if uid != "*":
                        continue
                if tid:
                    if add:
                        if (tid in cmd[bl]) is False:
                            cmd[bl][tid] = []
                        cmd[bl][tid].append(uid)
                    else:
                        if tid in cmd[bl]:
                            while uid in cmd[bl][tid]:
                                cmd[bl][tid].remove(uid)
                            if cmd[bl][tid] == []:
                                cmd[bl].pop(tid)
                else:
                    if add:
                        cmd[bl][uid] = 0
                    else:
                        while uid in cmd[bl]:
                            cmd[bl].pop(uid)

            a = [json.dumps(cmd[0]), json.dumps(cmd[1])]
            await self.change_perm(perm, a)
            return True
        return False

    async def check_perm(self, command, user_id, thread_id):
        a = await self.perm(command)
        user_id = str(user_id)
        thread_id = str(thread_id)
        # a[0] - whitelist
        # a[1] - blacklist
        if a:
            if "*" in a[1]:
                if user_id != thread_id:
                    if thread_id in a[0]:
                        if user_id in a[0][thread_id]:
                            return True
                        if "*" in a[0][thread_id]:
                            if user_id in a[1]:
                                return False
                            if thread_id in a[1]:
                                if user_id in a[1][thread_id]:
                                    return False
                            return True
                if user_id in a[0]:
                    if user_id != thread_id:
                        if thread_id in a[1]:
                            if user_id in a[1][thread_id]:
                                return False
                            if "*" in a[1][thread_id]:
                                return False
                    return True
                return False

            if "*" in a[0]:
                if user_id != thread_id:
                    if thread_id in a[1]:
                        if "*" in a[1][thread_id]:
                            if user_id in a[0]:
                                return True
                            if thread_id in a[0]:
                                if user_id in a[0][thread_id]:
                                    return True
                            return False
                        if user_id in a[1][thread_id]:
                            return False
                if user_id in a[1]:
                    if user_id != thread_id:
                        if thread_id in a[0]:
                            if user_id in a[0][thread_id]:
                                return True
                            if "*" in a[0][thread_id]:
                                return True
                    return False
                return True
            
            if user_id != thread_id:
                if thread_id in a[0]:
                    if "*" in a[0][thread_id]:
                        if thread_id in a[1]:
                            if user_id in a[1][thread_id]:
                                return False
                        return True
                    if user_id in a[0][thread_id]:
                        return True

            if user_id in a[0]:
                if a[0][user_id] == 0: # {'uid': {'uid': 0}} bug fix
                    return True
        return False

    async def standard_see(self, command, args):
        if len(command) > 0:
            try:
                n = int(command[1])
                if n > 15:
                    n = 15
            except (ValueError, IndexError):
                n = 1
            

            tid = args['thread_id']
            if len(command) == 3:
                try:
                    tid = int(command[2])
                except (ValueError, IndexError):
                    tid = args['thread_id']

            self.cur.execute("SELECT * FROM deletedMessages WHERE thread_id = ? ORDER BY time DESC LIMIT ?", [tid, n])
            a = self.cur.fetchall()
            for i in a:
                message_object = loads(i[5])
                n = 0
                for j in message_object.attachments:
                    b = j.__class__.__name__
                    name = config.attachments_location+message_object.uid+"_"+str(n)
                    if b == "ImageAttachment":
                        await self.send_local_files([name+'.jpg'], None, args["thread_id"], args["thread_type"])
                        n += 1 
                    elif b == "VideoAttachment":
                        await self.send_local_files([name+'.mp4'], None, args["thread_id"], args["thread_type"])
                        n += 1
                    elif b == "AudioAttachment":
                        await self.send_local_files([name+'.mp3'], None, args["thread_id"], args["thread_type"])
                        n += 1
                if message_object.text != None:
                    await self.send(message_object, args["thread_id"], args["thread_type"])
            return True
        await self.send(Message("Użycie:\n"+config.cmd_prefix+"see (liczba wiadomości) (tid)\nPokazuje N ostatnio usuniętych wiadomości."), args["thread_id"], args["thread_type"])

    async def saveMessage(self, message_object, args):
        self.cur.execute("INSERT INTO messages (mid, thread_id, author_id, time, message_object) VALUES (?, ?, ?, ?, ?)", [args["mid"], args["thread_id"], args["author_id"], args["ts"], dumps(message_object)])
        self.conn.commit()
        self.loop.create_task(self.saveAttachments(message_object))

    async def saveAttachments(self, message_object):
        n = 0
        for i in message_object.attachments:
            a = i.__class__.__name__
            if a == "ImageAttachment":
                try:
                    url = await self.fetch_image_url(i.uid)
                    img = requests.get(url).content
                except Exception as e:
                    print(e)
                    img = b''
                open(config.attachments_location+message_object.uid+'_'+str(n)+'.jpg', 'wb').write(img)
                n += 1 
            elif a == "VideoAttachment":
                try:
                    url = i.preview_url
                    video = requests.get(url).content
                except Exception as e:
                    video = b''
                    print(e)
                open(config.attachments_location+message_object.uid+'_'+str(n)+'.mp4', 'wb').write(video)
                n += 1
            elif a == "AudioAttachment":
                try:
                    url = i.url
                    audio = requests.get(url).content
                except Exception as e:
                    audio = b''
                    print(e)
                open(config.attachments_location+message_object.uid+'_'+str(n)+'.mp3', 'wb').write(audio)
                n += 1
        return True

    async def clearCron(self, t):
        # todo: maybe do class for handling 'cron like' execution
        if round(time.time()) - t[0] > 1200:
            self.cur.execute("SELECT time, mid FROM messages ORDER BY time ASC")
            a = self.cur.fetchall()
            b = round(time.time())*1000
            c = 1200000
            for i in a:
                if i[0] == None:
                    continue
                if b - int(i[0]) > c:
                    self.cur.execute("DELETE FROM messages WHERE mid = ?", [i[1]])
                    self.conn.commit()
                    for j in os.listdir(config.attachments_location):
                        if j.startswith(i[1]):
                            os.remove(config.attachments_location+j)

            t[0] = round(time.time())
        
        if round(time.time()) - t[1] > 300:
            self.cur.execute("SELECT end, id FROM notes")
            a = self.cur.fetchall()
            b = round(time.time())
            for i in a:
                if i[0] == "*":
                    pass
                else:
                    if int(i[0]) < b:
                        self.cur.execute("SELECT * FROM notes WHERE id = ?", [i[1]])
                        note = self.cur.fetchone()
                        
                        self.cur.execute("INSERT INTO deletedNotes (id, mid, uid, tid, start, end, note) VALUES (?, ?, ?, ?, ?, ?, ?)", [i[1], note[1], note[2], note[3], note[4], note[5], note[6]])
                        self.cur.execute("DELETE FROM notes WHERE id = ?", [i[1]])
                        self.conn.commit()
                        
            t[1] = round(time.time())
        return t

    async def on_message(self, mid=None, author_id=None, message_object=None, thread_id=None, thread_type=ThreadType.USER, at=None, **kwargs):
        args = {'action':'save_message', 'mid':mid, 'author_id':author_id, 'thread_id':thread_id, 'thread_type':thread_type, 'message_object':message_object, 'ts':int(datetime.datetime.timestamp(at))*1000}
        await self.mark_as_delivered(thread_id, message_object.uid)
        
        if author_id != self.uid:
            await self.saveMessage(message_object, args)

            if message_object.text == None:
                if len(message_object.attachments) > 0:
                    if message_object.attachments[0].__class__.__name__ == "ImageAttachment":
                        if thread_id in self.img_query:
                            if author_id in self.img_query[thread_id]:
                                tmp = self.img_query[thread_id][author_id]
                                if round(time.time()) < tmp[1]:
                                    await tmp[0](["img"], args, tmp[2])
                                self.img_query[thread_id].pop(author_id)
                                if self.img_query[thread_id] == {}:
                                    self.img_query.pop(thread_id)
                return True

            msg = message_object.text.lower()
            command = msg.split(" ")

        if await self.check_perm("banned", author_id, thread_id):
            pass
        else:
            if author_id != self.uid:

                if command[0][0:len(config.cmd_prefix)] == config.cmd_prefix:
                    if command[0][len(config.cmd_prefix):] in self.standard:
                        if await self.check_perm(command[0][len(config.cmd_prefix):], author_id, thread_id):
                            await self.standard[command[0][1:]](command, args)

                for cmd in self.special:
                    await cmd(command, args)

    async def on_message_unsent(self, mid=None, author_id=None, thread_id=None, thread_type=None, at=None, msg=None):
        args = {'action':'delete_message', 'mid':mid, 'author_id':author_id, 'thread_id':thread_id, 'thread_type':thread_type, 'ts':int(datetime.datetime.timestamp(at))*1000}
        self.cur.execute("SELECT * FROM messages WHERE mid = ?", [mid])
        b = self.cur.fetchone()
        self.cur.execute("INSERT INTO deletedMessages (mid, thread_id, author_id, time, del_time, message_object) VALUES (?, ?, ?, ?, ?, ?)", [mid, b[1], b[2], b[3], int(datetime.datetime.timestamp(at))*1000, b[4]])
        self.cur.execute("DELETE FROM messages WHERE mid = ?", [mid])
        self.conn.commit()

    async def on_reaction_added(self, mid=None, reaction=None, author_id=None, thread_id=None, thread_type=None, at=None, **kwargs):
        if author_id != self.uid:
            if at:
                at = int(datetime.datetime.timestamp(at))*1000
            else: 
                at = int(time.time()*1000)
            
            args = {"action":"add_reaction", "mid":mid, "reaction":reaction.value, "author_id":author_id, "thread_id":thread_id, "thread_type":thread_type.value, "ts":at}

            if await self.check_perm("doublereact", author_id, thread_id):
                await self.react_to_message(mid, reaction)

    async def on_reaction_removed(self, mid=None, author_id=None, thread_id=None, thread_type=ThreadType.USER, at=None, **kwargs):
        if author_id != self.uid:
            if at:
                at = int(datetime.datetime.timestamp(at))*1000
            else: 
                at = int(time.time()*1000)

            args = {"action":"remove_reaction", "mid":mid, "author_id":author_id, "thread_id":thread_id, "thread_type":thread_type.value, "ts":at}
            
            if await self.check_perm("doublereact", author_id, thread_id):
                await self.react_to_message(mid, None)

    async def on_nickname_change(self, author_id=None, changed_for=None, new_nickname=None, thread_id=None, thread_type=ThreadType.USER, at=None, **kwargs):
        args = {"action":"change_nickname", "author_id":author_id, "changed_for":changed_for, "new_nickname":new_nickname, "thread_id":thread_id, "thread_type":thread_type.value, "ts":int(datetime.datetime.timestamp(at))*1000}
        if author_id != self.uid:
            if await self.check_perm("usunnick", changed_for, thread_id):
                await self.change_nickname(".", changed_for, thread_id)
                await self.standard_szkaluj(["!szkaluj"], {'author_id':author_id, 'thread_id':thread_id, 'thread_type':thread_type})

    # async def on_color_change(self, author_id=None, new_color=None, thread_id=None, thread_type=ThreadType.USER, at=None, **kwargs):
    #     args = {"action":"change_color", "author_id":author_id, "new_color":new_color, "thread_id":thread_id, "thread_type":thread_type.value, "ts":int(datetime.datetime.timestamp(at))*1000}

    # async def on_emoji_change(self, author_id=None, new_emoji=None, thread_id=None, thread_type=ThreadType.USER, at=None, **kwargs):
    #     args = {"action":"change_emoji", "author_id":author_id, "new_emoji":new_emoji, "thread_id":thread_id, "thread_type":thread_type.value, "ts":int(datetime.datetime.timestamp(at))*1000}

    # async def on_title_change(self, author_id=None, new_title=None, thread_id=None, thread_type=ThreadType.USER, at=None, **kwargs):
    #     args = {"action":"change_title", "author_id":author_id, "new_title":new_title, "thread_id":thread_id, "thread_type":thread_type.value, "ts":int(datetime.datetime.timestamp(at))*1000}

    # async def on_admin_added(self, added_id=None, author_id=None, thread_id=None, thread_type=ThreadType.GROUP, at=None, **kwargs):
    #     args = {"action":"add_admin", "author_id":author_id, "added_id":added_id, "thread_id":thread_id, "thread_type":thread_type.value, "ts":int(datetime.datetime.timestamp(at))*1000}

    # async def on_admin_removed(self, removed_id=None, author_id=None, thread_id=None, thread_type=ThreadType.GROUP, at=None, **kwargs):
    #     args = {"action":"remove_admin", "author_id":author_id, "removed_id":removed_id, "thread_id":thread_id, "thread_type":thread_type.value, "ts":int(datetime.datetime.timestamp(at))*1000}

    # async def on_approval_code_change(self, approval_mode=None, author_id=None, thread_id=None, thread_type=ThreadType.GROUP, at=None, **kwargs):
    #     args = {"action":"change_approval", "author_id":author_id, "approval_mode":approval_mode, "thread_id":thread_id, "thread_type":thread_type.value, "ts":int(datetime.datetime.timestamp(at))*1000}

    async def on_person_removed(self, removed_id=None, author_id=None, thread_id=None, at=None, **kwargs):
        args = {"action":"add_people", "author_id":author_id, "removed_id":removed_id, "thread_id":thread_id, "ts":int(datetime.datetime.timestamp(at))*1000}
        if removed_id == mojeid:
            pass
        else:
            await self.send(Message("poziom wzrus"), thread_id, ThreadType.GROUP)

    async def on_people_added(self, added_ids=None, author_id=None, thread_id=None, at=None, **kwargs):
        args = {"action":"add_people", "author_id":author_id, "added_ids":added_ids, "thread_id":thread_id, "ts":int(datetime.datetime.timestamp(at))*1000}
        if mojeid in added_ids or self.uid in added_ids:
            pass
        else:    
            await self.send(Message("poziom spat"), thread_id, ThreadType.GROUP)

    # async def on_image_change(self, author_id=None, new_image=None, thread_id=None, thread_type=ThreadType.GROUP, at=None, **kwargs):
    #     new_image = await self.fetch_image_url(new_image)
    #     new_image = base64.b64encode(requests.get(new_image).content)
    #     args = {"action":"change_image", "author_id":author_id, "new_image":new_image, "thread_id":thread_id, "thread_type":thread_type, "ts":int(datetime.datetime.timestamp(at))*1000}

    async def server_uptime(self):
        r = subprocess.run(["sysctl", "kern.boottime"], stdout=subprocess.PIPE)
        r = r.stdout.decode()
        return round(time.time()) - int(r[r.find("= ")+2:r.find(", ")])

    async def main(self):
        self.mb = MobiDziennik(config.mb_subdomain, config.mb_login, config.mb_password)
        self.conn = sqlite3.connect(config.db_name)
        self.cur = self.conn.cursor()
        
        # todo: make automatic permissions for commands not existing in db
        # create tables if they don't exist
        db_schema = open('db_schema.sql', 'r').read()
        self.cur.executescript(db_schema)
        
        for command_name in dir(self):
            if command_name.startswith("standard_"):
                self.standard[command_name.replace("standard_", "")] = getattr(self, command_name)
            if command_name.startswith("special_"):
                self.special.append(getattr(self, command_name))

        # todo: fix session file (doesn't work at all)
        sess_f = open("fb_session", "wb+")
        try:
            ses = load(sess_f) # load session
        except:
            ses = None

        # await self.start(config.email, config.password)
        user_agent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.6 (KHTML, like Gecko) Chrome/20.0.1092.0 Safari/536.6"

        if (
            not ses
            or not await self.set_session(ses, user_agent)
            or not await self.is_logged_in()
        ):
            await self.login(config.email, config.password, user_agent)

        ses = self.get_session()
        dump(ses, sess_f) # save session

        self.loop.create_task(self._listen_mqtt())

        # todo: make a better solution
        t = [0, 0]
        schedule.every().day.at("04:00").do(self.dailyNumber)
        while True:
            t = await self.clearCron(t)

            try:
                schedule.run_pending()
            except Exception as e:
                print(f'\nSchedule run pending exception\n{ e }')
            await asyncio.sleep(30)


if __name__ == '__main__':
    loop = asyncio.get_event_loop()

    a = logging.basicConfig(format='%(levelname)s:%(message)s', level=logging.WARNING)
    bot = WiertarBot(loop=loop, log=a)

    loop.run_until_complete(bot.main())
    # loop.stop()
    # loop.close()
