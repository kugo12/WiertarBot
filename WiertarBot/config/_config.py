import asyncio

from returns.curry import partial
from dataclasses import dataclass
from os import PathLike, environ
from typing import TypeVar, Any, Union, Type, Optional, cast, Final, Callable, overload, Literal, Coroutine, Awaitable, \
    Generic, get_args
from typing_extensions import ParamSpec, Concatenate
from inspect import isawaitable, getfullargspec

import dacite
import yaml
import re

from WiertarBot.config._utils import traverse_dict

_T = TypeVar("_T")
_P = ParamSpec("_P")

_on_T = TypeVar("_on_T")
_on_T_ret = TypeVar("_on_T_ret")

env_regex = re.compile(r"\$(?P<b>{)?(?P<env>[A-Za-z_]+)(?(b)})")

InitType = Callable[[], Union[Awaitable[None], None]]


def expand_env(string: str) -> str:
    for match in env_regex.finditer(string):
        name = match.group("env")
        expanded = environ.get(name)

        string = string.replace(match.group(), expanded or "", 1)

    return string


def _empty(*args, **kwargs) -> None:
    return


async def _async_empty(*args, **kwargs) -> None:
    return


class MissingConfigProperties(Exception):
    pass


_CI = TypeVar("_CI")


def _get_first_arg_type(func: Callable) -> Optional[type[object]]:
    spec = getfullargspec(func)

    try:
        return cast(type[object], spec.annotations[spec.args[0]])
    except KeyError:
        return None


class ConfigInject(Generic[_CI]):
    def __init__(self, value: Optional[_CI]) -> None:
        self.value = value

    @overload
    def __call__(self, func: Callable[Concatenate[_CI, _P], Awaitable[_on_T_ret]]) -> Callable[_P, Awaitable[Optional[_on_T_ret]]]: ...  # type: ignore

    @overload
    def __call__(self, func: Callable[_P, Awaitable[_on_T_ret]]) -> Callable[_P, Awaitable[Optional[_on_T_ret]]]: ...  # type: ignore

    @overload
    def __call__(self, func: Callable[Concatenate[_CI, _P], _on_T_ret]) -> Callable[_P, Optional[_on_T_ret]]: ...

    @overload
    def __call__(self, func: Callable[_P, _on_T_ret]) -> Callable[_P, Optional[_on_T_ret]]: ...

    def __call__(self, func: Callable) -> Callable:
        if self.value is None:
            return _async_empty if isawaitable(func) else _empty

        pass_value = _get_first_arg_type(func) == self.value.__class__

        return partial(func, self.value) if pass_value else func



class Config:
    _raw: dict[str, Any]
    _mapping: dict[type, Any]
    _init: list[InitType]

    def __init__(self, path: Union[str, PathLike]) -> None:
        self._mapping = {}
        self._init = []

        with open(path, "r") as f:
            config_str = f.read()

        config_str = expand_env(config_str)
        self._raw = yaml.safe_load(config_str)

    def properties(self, prefix: str, *, optional: bool = False):
        keys = prefix.split(".")
        mapping: Final = traverse_dict(self._raw, keys)

        if mapping and isinstance(mapping, dict):
            def wrap(cls: Type[_T]) -> Type[_T]:
                cls = dataclass(cls)
                self._mapping[cls] = dacite.from_dict(data_class=cls, data=cast(dict[str, Any], mapping))
                return cls

            return wrap
        elif mapping is None and optional:
            return dataclass
        else:
            raise MissingConfigProperties(f"Missing config properties {prefix}")

    def get(self, item: Type[_T]) -> Optional[_T]:
        cls = self._mapping.get(item)

        if cls and not isinstance(cls, item):
            raise TypeError(f"Expected type {item} but item was {type(cls)}")

        return cls

    def __getitem__(self, item: Type[_T]) -> _T:
        output = self.get(item)

        if output is None:
            raise MissingConfigProperties(f"Missing config object {item}")

        return output

    # typing stuff, mypy support for pep646 not there yet, not sure if it would solve this problem
    # https://github.com/python/mypy/issues/12280

    def on(self, value_type: type[_T]) -> ConfigInject[_T]:
        value = self.get(value_type)

        return ConfigInject(value)

    async def init(self) -> None:
        returns = [it() for it in self._init]
        await asyncio.gather(*[it for it in returns if it])
        self._init = []

    def register_init(self, func: InitType) -> None:
        self._init.append(func)
