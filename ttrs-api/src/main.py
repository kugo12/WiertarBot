import sentry_sdk
import dacite
from flask import Flask, Response, request
from translatepy import Translate
from gtts import gTTS
from gtts.lang import tts_langs
from dataclasses import dataclass
from translatepy.exceptions import UnknownLanguage

sentry_sdk.init(sample_rate=1.0)
app = Flask(__name__)
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


@app.route('/api/tts', methods=['POST'])
def tts():
    data = dacite.from_dict(
        TTSRequest,
        request.get_json()
    )

    lang = data.lang or "en"
    if not data.text or lang not in tts_langs():
        return Response(status=422)

    audio = gTTS(data.text, lang=lang).stream()

    return app.response_class(audio, mimetype="audio/mp4")


@app.route('/api/tts/lang')
def _tts_languages():
    return tts_languages


@app.route("/api/translate", methods=["POST"])
def translate():
    data = dacite.from_dict(
        TranslateRequest,
        request.get_json()
    )

    try:
        result = Translate().translate(data.text, data.destination, data.source or "auto")
    except UnknownLanguage:
        return Response(status=422)

    return Response(result.result)


app.route('/health')(lambda: Response(status=200))

if __name__ == '__main__':
    app.run()
