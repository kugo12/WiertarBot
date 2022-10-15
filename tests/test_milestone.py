from typing import Optional

import pytest

from WiertarBot.milestone import _check_threshold, _default_total_delta
from sys import maxsize

_x_total_delta = 20 * _default_total_delta


@pytest.mark.parametrize(
    "args,expected",
    [
        ((0, 0), None),
        ((maxsize, 0), None),
        ((0, _x_total_delta - 1), _x_total_delta - _default_total_delta),
        ((0, _default_total_delta), _default_total_delta),
        ((0, _x_total_delta + 1), _x_total_delta),
        ((_x_total_delta - 2, _x_total_delta - 1), None),
        ((0, _x_total_delta), _x_total_delta),
        ((0, 1, 1), 1),
        ((0, 2, 1), 2),
        ((5, 6, 3), 6),
        ((4, 5, 3), None),
    ]
)
def test_check_threshold(
    args: tuple[int, ...], expected: Optional[int]
):
    assert _check_threshold(*args) == expected
