import requests
from bs4 import BeautifulSoup
from os import path


# CONSTANTS
USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.81 Safari/537.36"
BASE_URL = "https://theneedledrop.com/"
MBDTF_REVIEW_URL = "https://www.theneedledrop.com/articles/2020/1/kanye-west-my-beautiful-dark-twisted-fantasy"


class Fantano:
    def __init__(self):
        self.s = requests.Session()
        self.s.headers.update({"User-Agent": USER_AGENT})

    def _search_review(self, term: str) -> str:
        """
            Returns first search result
        """
        url = path.join(BASE_URL, "search")
        params = {'q': term}
        # Search for term
        search_response = self.s.get(url, params=params, verify=False)
        parsed_search_results = BeautifulSoup(search_response.text, "html.parser")
        # Get first result
        try:
            review_uri = parsed_search_results.find("div", class_="search-result")["data-url"][1:]  # type: ignore
        except TypeError:
            # Tak, to celowy zabieg
            return MBDTF_REVIEW_URL
        else:
            url = path.join(BASE_URL, review_uri)  # type: ignore
        return url
        
    def _get_rate_from_url(self, url: str):
        """
            Returns rating from url of article
        """
        response = self.s.get(url)
        parsed = BeautifulSoup(response.text, "html.parser")
        title = parsed.find(class_="entry-title").text.replace('\n', '')  # type: ignore

        rate = None
        for tag in parsed.find("span", class_="entry-tags").contents:  # type: ignore
            if "/10" in tag.text:
                rate = tag.text
                break

        review = parsed.p.text  # type: ignore
        return {
            "title": title,
            "review": review,
            "rate": rate
        }
                
    def get_rate(self, term: str):
        return self._get_rate_from_url(self._search_review(term))

