from requests import post
from json import loads
from datetime import datetime
from sys import argv
import cloudscraper

class AliPaczka():
    def __init__(self, number):
        self.number = str(number)
        self.tracking = self.get_tracking()

    def get_tracking(self):
        p = cloudscraper.create_scraper().post("https://api.alipaczka.pl/track/"+self.number+"/", data={"uid": "2222", "ver":"22"}).text
        print(p)
        p = loads(p)
        if 'error' in p:
            return p
        for i in p['DataEntry']:
            i['time'] = datetime.fromtimestamp(int(i['time']))
        return p

    def __str__(self):
        out = ""
        if "error" in self.tracking:
            return self.tracking["error"]

        out += "Numer paczki: "+self.number+"\nDostarczona: "
        out += "tak" if self.tracking['isDelivered'] else "nie"
        for i in self.tracking['DataEntry']:
            out += "\n"+i['time'].strftime("%d/%m/%Y %H:%M")+": "+i['status']
        return out

if __name__ == "__main__":
    print(AliPaczka(argv[1]))
