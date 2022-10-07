from typing import Any, Union

TraversedValue = Union[dict[str, Any], Any, None]


def traverse_dict(obj: dict[str, Any], path: list[str]) -> TraversedValue:
    mapping: TraversedValue = obj

    for key in path:
        if mapping and key in mapping:
            mapping = mapping[key]
        else:
            return None

    return mapping
