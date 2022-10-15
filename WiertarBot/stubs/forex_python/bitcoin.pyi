from .converter import DecimalFloatMismatchError as DecimalFloatMismatchError, RatesNotAvailableError as RatesNotAvailableError
from _typeshed import Incomplete

class BtcConverter:
    def __init__(self, force_decimal: bool = ...) -> None: ...
    def get_latest_price(self, currency): ...
    def get_previous_price(self, currency, date_obj): ...
    def get_previous_price_list(self, currency, start_date, end_date): ...
    def convert_to_btc(self, amount, currency): ...
    def convert_btc_to_cur(self, coins, currency): ...
    def convert_to_btc_on(self, amount, currency, date_obj): ...
    def convert_btc_to_cur_on(self, coins, currency, date_obj): ...
    def get_symbol(self): ...

get_btc_symbol: Incomplete
convert_btc_to_cur_on: Incomplete
convert_to_btc_on: Incomplete
convert_btc_to_cur: Incomplete
convert_to_btc: Incomplete
get_latest_price: Incomplete
get_previous_price: Incomplete
get_previous_price_list: Incomplete
