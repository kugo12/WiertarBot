from datetime import datetime
from typing import cast

import requests


class AliPaczka:
    __slots__ = ['tracking', 'number']

    def __init__(self, number: str) -> None:
        self.number = number
        self.tracking = self.get_tracking()

    def get_tracking(self) -> dict:
        p = requests.post(
            f'https://api.alipaczka.pl/track/{self.number}/',
            data={"uid": "2222", "ver": "22"}
        ).json()
        if 'error' not in p:
            for i in p['DataEntry']:
                i['time'] = datetime.fromtimestamp(int(i['time']))
        return cast(dict, p)

    def __str__(self):
        out = ''
        if 'error' in self.tracking:
            return self.tracking['error']

        out += f'Numer paczki: {self.number}\nDostarczona: '
        out += 'tak' if self.tracking['isDelivered'] else 'nie'
        for i in self.tracking['DataEntry']:
            out += f'\n{i["time"].strftime("%d/%m/%Y %H:%M")}: {i["status"]}'

        return out


if __name__ == "__main__":
    from sys import argv

    print(AliPaczka(argv[1]))
