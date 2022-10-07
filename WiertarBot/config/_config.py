from dataclasses import dataclass
from os import PathLike
from typing import TypeVar, Any, Union, Type, Optional, cast, Final, Dict

import dacite
import yaml

from WiertarBot.config._utils import traverse_dict

_T = TypeVar("_T")


class MissingConfigProperties(Exception):
    pass


class Config:
    _raw: dict[str, Any]
    _mapping: dict[type, Any]

    def __init__(self, path: Union[str, PathLike]):
        self._mapping = {}

        with open(path, "r") as f:
            self._raw = yaml.safe_load(f)

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
