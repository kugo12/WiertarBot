from _typeshed import Incomplete

class RatesNotAvailableError(Exception): ...
class DecimalFloatMismatchError(Exception): ...

class Common:
    def __init__(self, force_decimal: bool = ...) -> None: ...

class CurrencyRates(Common):
    def get_rates(self, base_cur, date_obj: Incomplete | None = ...): ...
    def get_rate(self, base_cur, dest_cur, date_obj: Incomplete | None = ...): ...
    def convert(self, base_cur, dest_cur, amount, date_obj: Incomplete | None = ...): ...

get_rates: Incomplete
get_rate: Incomplete
convert: Incomplete

class CurrencyCodes:
    def __init__(self) -> None: ...
    def get_symbol(self, currency_code): ...
    def get_currency_name(self, currency_code): ...
    def get_currency_code_from_symbol(self, symbol): ...

get_symbol: Incomplete
get_currency_name: Incomplete
get_currency_code_from_symbol: Incomplete
