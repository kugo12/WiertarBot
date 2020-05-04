import requests, pickle, time, os, json
from bs4 import BeautifulSoup

class IncorrectSubdomain(Exception):
    def __init__(self, subdomain):
        self.subdomain = subdomain
    def __str__(self):
        return "Subdomain doesn't exist (" + self.subdomain + ")"

class MobiDziennik():
    def __init__(self, subdomain, login, password, session_file="mobidziennik/session"):
        if os.path.isdir("mobidziennik/") == False:
            os.mkdir("mobidziennik")
        self.address = "https://"+subdomain+".mobidziennik.pl/dziennik"
        self.login = login
        self.password = password
        self.session_file = session_file
        self.fl = open(session_file, "wb+")
        
        response = requests.head(self.address, allow_redirects=True).url
        if "zlyadres.php" in response:
            raise IncorrectSubdomain(subdomain)
        else:
            if os.path.isfile(session_file):
                try:
                    ses = pickle.load(self.fl)
                    self.s = ses
                    return None
                except:
                    pass
            
            self.s = requests.Session()
            self.s.post(self.address, data={'login': self.login, 'haslo': self.password}, allow_redirects=True)
            pickle.dump(self.s, self.fl)
                

    def checkSession(self):
        response = self.s.get(self.address+"/bazaplikow", allow_redirects=True)
        if response.url != self.address+"/bazaplikow" or "id=\"login\" name=\"login\" type=\"text\"" in response.text:
            self.newSession()
            
    def newSession(self):
        self.s = requests.Session()
        self.s.post(self.address, data={'login': self.login, 'haslo': self.password}, allow_redirects=True)
        pickle.dump(self.s, self.fl)

    def getLuckyNumber(self):
        self.checkSession()
        response = self.s.get(self.address+"/bazaplikow").text
        parsed = BeautifulSoup(response, "html.parser")
        lucky = parsed.body.find("a", "szczesliwy_numerek").get_text().split("/")[0]

        return lucky

    def getSubstitutions(self):
        self.checkSession()
        response = self.s.get(self.address+"/zastepstwa").text
        parsed = BeautifulSoup(response, "html.parser")
        parsed = parsed.body.find_all("tr", "komorka_biala")

        subs = []
        for sub in parsed:
            a = []
            for td in sub.find_all("td"):
                a.append(td.get_text())
            subs.append(a)

        return subs

    def getHomework(self):
        self.checkSession()
        response = self.s.get(self.address+"/zadaniadomowe").text
        parsed = BeautifulSoup(response, "html.parser")
        parsed = parsed.body.find("table", "spis").find_all("tr")[1:]

        homeworks = []
        for homework in parsed:
            a = homework.find_all("td")
            b = []
            b.append(a[0].get_text().replace(a[0].small.get_text(), "").strip())
            b.append(a[1].a.span.get_text())
            b.append(a[2].a.span.get_text())
            b.append(a[3].get_text())
            #a[4] - kto wpisal i kiedy
            homeworks.append(b)

        return homeworks

    def getTests(self):
        self.checkSession()
        response = self.s.get(self.address+"/sprawdziany").text
        parsed = BeautifulSoup(response, "html.parser")
        parsed = parsed.body.find("table", "spis").find_all("tr")[2:]
        
        tests = []
        for test in parsed:
            p = test.find_all("td")
            b = []
            b.append(p[0].get_text().replace(p[0].small.get_text(), "").strip())
            b.append(p[1].span["title"])
            b.append(p[2].span["title"])
            b.append(p[3].span["title"])
            b.append(p[4].get_text())
            tests.append(b)

        return tests

    def getCalendar(self, date=round(time.time())):
        self.checkSession()
        response = self.s.get(self.address+"/kalendarzklasowy")
        parsed = BeautifulSoup(response.text, "html.parser")
        script = parsed.find_all("script", attrs={"src":""})[1].text
        script = script[script.find("events: [")+8:script.rfind("}],")+2]
        
        events = json.loads(script)
        if date == "all":
            return events
            
        date = time.mktime(time.strptime(time.strftime("%Y-%m-%d", time.gmtime(date)), "%Y-%m-%d"))
        ev = []
        for event in events:
            if time.mktime(time.strptime(event["start"], "%Y-%m-%d")) < date:
                try:
                    if time.mktime(time.strptime(event["end"], "%Y-%m-%d")) >= date:
                        ev.append(event)
                except KeyError:
                    pass
            else:
                ev.append(event)
        return ev

    # def getDuties(self, date=round(time.time())):
    #     events = self.getCalendar(date)
    #     duties = []
    #     for event in events:
    #         if event["color"] == "#EFD459":
    #             duties.append(event)
    #     return duties

    # def getDuty(self, date=round(time.time())):
    #     duties = self.getDuties(date)
    #     date = time.mktime(time.strptime(time.strftime("%Y-%m-%d", time.gmtime(date)), "%Y-%m-%d"))
    #     duty = []
    #     for d in duties:
    #         if time.mktime(time.strptime(d["end"], "%Y-%m-%d")) >= date:
    #             if time.mktime(time.strptime(d["start"], "%Y-%m-%d")) <= date:
    #                 duty.append(d)
    #     if duty == []:
    #         a = None
    #         for d in duties:
    #             if a == None:
    #                 a = d
    #             elif time.mktime(time.strptime(a["start"], "%Y-%m-%d")) > time.mktime(time.strptime(d["start"], "%Y-%m-%d")):
    #                 a = d
    #         duty.append(a)
    #         for d in duties:
    #             if duty[0]["start"] == d["start"]:
    #                 if duty[0] != d:
    #                     duty.append(d)

    #     dut = []
    #     for a in duty:
    #         dut.append({"who":a["title"].replace("Dyżurny - ", ""), "start":a["start"], "end":a["end"]})
        
    #     return dut

    def getClass(self):
        self.checkSession()
        response = self.s.post(self.address+"/odbiorcyWiadomosci", data={"typ":"4", "odpowiedz":0})
        parsed = BeautifulSoup(response.text, "html.parser")
        parsed = parsed.find_all("label") #Osoby z klasy

        cl = {}
        n = 1 #Numer w dzienniku
        for person in parsed:
            name = person.span.text #Imie i nazwisko 
            ID = person.input["value"] #ID z mobidziennika
            cl[n] = {"ID":ID, "name":name}
            n += 1
            
        return cl

    def getScheduleTime(self):
        self.checkSession()
        response = self.s.get(self.address+"/planlekcji")
        parsed = BeautifulSoup(response.text, "html.parser").find("div", {"class": "plansc_godz"}).find_all("span")

        lesson_time = {}
        for t in parsed:
            if t.b == None:
                lesson_time[b]["end"] = t.text[:5]
            else:
                b = t.b.text
                lesson_time.update({b:{"start":None, "end": None}})
                lesson_time[b]["start"] = t.text[:5]

        return lesson_time

