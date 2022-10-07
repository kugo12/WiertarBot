from _typeshed import Incomplete

class Translated:
    src: Incomplete
    confidence: Incomplete
    dest: Incomplete
    origin: Incomplete
    text: Incomplete
    pronunciation: Incomplete
    def __init__(self, src, confidence, dest, origin, text, pronunciation) -> None: ...
    def __unicode__(self): ...

class Detected:
    lang: Incomplete
    confidence: Incomplete
    def __init__(self, lang, confidence) -> None: ...
    def __unicode__(self): ...
