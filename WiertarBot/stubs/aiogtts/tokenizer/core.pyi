from _typeshed import Incomplete

class RegexBuilder:
    pattern_args: Incomplete
    pattern_func: Incomplete
    flags: Incomplete
    regex: Incomplete
    def __init__(self, pattern_args, pattern_func, flags: int = ...) -> None: ...

class PreProcessorRegex:
    repl: Incomplete
    regexes: Incomplete
    def __init__(self, search_args, search_func, repl, flags: int = ...) -> None: ...
    def run(self, text): ...

class PreProcessorSub:
    pre_processors: Incomplete
    def __init__(self, sub_pairs, ignore_case: bool = ...): ...
    def run(self, text): ...

class Tokenizer:
    regex_funcs: Incomplete
    flags: Incomplete
    total_regex: Incomplete
    def __init__(self, regex_funcs, flags=...) -> None: ...
    def run(self, text): ...
