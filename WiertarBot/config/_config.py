from dataclasses import dataclass
from os import PathLike
from typing import TypeVar, Any, Union, Type, Optional

import dacite
import yaml

_T = TypeVar("_T")


class MissingConfigProperties(Exception):
    pass


class Config:
    _raw: dict[str, Any]
    _mapping: dict[type, Any]

    def __init__(self, path: Union[str, PathLike]):
        self._mapping = dict()

        with open(path, "r") as f:
            self._raw = yaml.safe_load(f)

    def properties(self, prefix: str, *, optional: bool = False):
        keys = prefix.split(".")
        mapping = self._raw

        for key in keys:
            if mapping is not None and len(key) != 0:
                mapping = mapping.get(key)

        if mapping is None:
            if optional:
                return lambda cls: dataclass(cls)
            else:
                raise MissingConfigProperties(f"Missing config properties {prefix}")

        def wrap(cls: Type[Any]):
            cls = dataclass(cls)
            self._mapping[cls] = dacite.from_dict(data_class=cls, data=mapping)
            return cls

        return wrap

    def __getitem__(self, item: Type[_T]) -> Optional[_T]:
        cls = self._mapping.get(item)

        if cls is not None and not isinstance(cls, item):
            raise TypeError(f"Expected type {item} but item was {type(cls)}")

        return cls

    def get(self, item: Type[_T]) -> _T:
        output = self[item]

        if output is None:
            raise MissingConfigProperties(f"Missing config object {item}")

        return output
