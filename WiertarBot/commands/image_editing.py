import os
from io import BytesIO
from typing import BinaryIO
from PIL import Image, ImageEnhance, ImageOps, ImageFont, ImageDraw

from .ABCImageEdit import ImageEditABC
from ..config import cmd_media_path
from ..dispatch import MessageEventDispatcher
from ..log import log


@MessageEventDispatcher.register()
class wypierdalaj(ImageEditABC):
    """
    Użycie:
        {command}
    Zwraca:
        przerobione zdjęcie z WYPIERDALAJ
    Informacje:
        działa również na zdjęcia z odpowiedzi
    """
    template_path = str(cmd_media_path / 'templates/wypierdalaj.jpg')

    async def edit(self, fp: BinaryIO) -> BinaryIO:
        img = Image.open(fp)
        new = Image.new("RGB", img.size)
        wyp = Image.open(self.template_path)

        h = round(img.height/8)

        wyp = wyp.resize((img.width, h))
        new.paste(wyp, (0, img.height-h))
        img = img.resize((img.width, img.height-h))
        new.paste(img, (0, 0))

        f = BytesIO()
        new.save(f, format='JPEG', quality=97)
        f.seek(0)

        return f


@MessageEventDispatcher.register(name='2021')
class _2021(ImageEditABC):
    """
    Użycie:
        {command}
    Zwraca:
        przerobione zdjęcie z templatem 2021
    Informacje:
        działa również na zdjęcia z odpowiedzi
    """
    template_path = str(cmd_media_path / 'templates/2021.jpg')

    async def edit(self, fp: BinaryIO) -> BinaryIO:
        tps = [500, 179]
        img = Image.open(fp)
        ims = [img.width, img.height]
        tmpl = Image.open(self.template_path)

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

        f = BytesIO()
        new.save(f, format='JPEG', quality=97)
        f.seek(0)

        return f


@MessageEventDispatcher.register()
class deepfry(ImageEditABC):
    """
    Użycie:
        {command}
    Zwraca:
        usmażone zdjęcie
    Informacje:
        działa również na zdjęcia z odpowiedzi
    """
    template_flare1 = str(cmd_media_path / 'templates/flara.png')
    template_flare2 = str(cmd_media_path / 'templates/flara2.png')

    async def edit(self, fp: BinaryIO) -> BinaryIO:
        img = Image.open(fp).convert("RGB")
        fl = Image.open(self.template_flare1).convert("RGBA")
        fl2 = Image.open(self.template_flare2).convert("RGBA")

        fl = fl.resize((round(2*fl.width*fl.width/img.width),
                        round(2*fl.height*fl.height/img.height)))

        fl2 = fl2.resize((round(2*fl2.width*fl2.width/img.width),
                          round(2*fl2.height*fl2.height/img.height)))

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

        f = BytesIO()
        img.save(f, format='JPEG', quality=50)
        f.seek(0)

        return f


@MessageEventDispatcher.register()
class nobody(ImageEditABC):
    """
    Użycie:
        {command} (tekst)
    Zwraca:
        przerobione zdjęcie z
    Informacje:
        działa również na zdjęcia z odpowiedzi
    """
    arial_path = str(cmd_media_path / 'arial.ttf')

    async def edit(self, fp: BinaryIO) -> BinaryIO:
        del self.args[0]
        if self.args:
            text = ' '.join(self.args)
        else:
            text = 'Nobody:\n\nMe:   '

        font = ImageFont.truetype(self.arial_path, 44)

        img = Image.open(fp).convert("RGB")
        draw = ImageDraw.Draw(img)
        w, h = draw.textsize(text, font=font)
        h = h+22
        w = round(w*1.7)
        tps = [w, h]
        tmpl = Image.new("RGB", tps, "#fff")
        draw = ImageDraw.Draw(tmpl)
        draw.text((0, 0), text, font=font, fill="#000")
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

        f = BytesIO()
        new.save(f, format='JPEG', quality=97)
        f.seek(0)

        return f
