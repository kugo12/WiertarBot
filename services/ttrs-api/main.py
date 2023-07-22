from asyncio import to_thread
from dataclasses import dataclass

import dacite
import sentry_sdk
from aiohttp import web
from gtts import gTTS
from gtts.lang import tts_langs
from sentry_sdk.integrations.aiohttp import AioHttpIntegration
from translatepy import Translate
from translatepy.exceptions import UnknownLanguage

routes = web.RouteTableDef()
tts_languages = tts_langs()


@dataclass
class TTSRequest:
    text: str
    lang: str | None


@dataclass
class TranslateRequest:
    text: str
    source: str | None
    destination: str


@routes.post('/api/tts')
async def tts(request: web.Request) -> web.StreamResponse:
    data = dacite.from_dict(
        TTSRequest,
        await request.json()
    )

    lang = data.lang or "en"
    if not data.text or lang not in tts_langs():
        raise web.HTTPUnprocessableEntity()

    audio = await to_thread(lambda: gTTS(data.text, lang=lang).stream())

    response = web.StreamResponse()
    response.content_type = "audio/mp4"

    await response.prepare(request)
    for it in audio:
        await response.write(it)
    await response.write_eof()

    return response


@routes.get('/api/tts/lang')
async def _tts_languages(_) -> web.Response:
    return web.json_response(tts_languages)


@routes.post("/api/translate")
async def translate(request: web.Request) -> web.Response:
    data = dacite.from_dict(
        TranslateRequest,
        await request.json()
    )

    try:
        result = await to_thread(lambda: Translate().translate(data.text, data.destination, data.source or "auto"))
    except UnknownLanguage:
        raise web.HTTPUnprocessableEntity()

    return web.Response(body=result.result)


@routes.get('/health')
async def health(_) -> web.Response:
    return web.Response()


app = web.Application()
app.add_routes(routes)

if __name__ == "__main__":
    sentry_sdk.init(
        integrations=[
            AioHttpIntegration(),
        ],
        sample_rate=1.0
    )
    web.run_app(app, host="0.0.0.0", port=8080)
