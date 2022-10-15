import json
from ._common import log as log
from _typeshed import Incomplete

FLAGS: Incomplete
WHITESPACE: Incomplete

class ConcatJSONDecoder(json.JSONDecoder):
    def decode(self, s, _w=...): ...

def queries_to_json(*queries): ...
def response_to_json(text): ...
def from_query(query, params): ...
def from_query_id(query_id, params): ...
def from_doc(doc, params): ...
def from_doc_id(doc_id, params): ...

FRAGMENT_USER: str
FRAGMENT_GROUP: str
FRAGMENT_PAGE: str
SEARCH_USER: Incomplete
SEARCH_GROUP: Incomplete
SEARCH_PAGE: Incomplete
SEARCH_THREAD: Incomplete
