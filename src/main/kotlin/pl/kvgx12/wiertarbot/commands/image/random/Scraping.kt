package pl.kvgx12.wiertarbot.commands.image.random

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import it.skrape.core.htmlDocument
import pl.kvgx12.wiertarbot.command.command
import pl.kvgx12.wiertarbot.command.commands
import pl.kvgx12.wiertarbot.events.Response

val randomImageScrapingCommands = commands {
    command {
        name = "suchar"
        help(returns = "zdjęcie z sucharem")

        generic { event ->
            val response = client.get("https://www.suchary.com/random.html").bodyAsText()

            val url = htmlDocument(response) {
                findFirst(".file-container a img") {
                    attribute("src")
                }
            }

            Response(event, files = event.context.upload(url))
        }
    }

// FIXME:
//    @MessageEventDispatcher.register(aliases=['jeż'])
//    async def jez(event: MessageEvent) -> IResponse:
//        """
//        Użycie:
//            {command}
//        Zwraca:
//            zdjęcie z jeżykiem
//        """
//        url = f'http://www.cutestpaw.com/tag/hedgehogs/page/{random.randint(1, 10)}/'
//        r = requests.get(url)
//        bs = BeautifulSoup(r.text, 'html.parser')
//        h = bs.find_all('a', {'title': True})
//        image_url = random.choice(h).img['src']  # type: ignore
//        return await file_upload_response(event, [image_url])

}

private val client = HttpClient(CIO)
