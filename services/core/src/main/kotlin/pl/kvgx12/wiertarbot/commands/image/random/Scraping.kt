package pl.kvgx12.wiertarbot.commands.image.random

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import it.skrape.core.htmlDocument
import pl.kvgx12.wiertarbot.command.command
import pl.kvgx12.wiertarbot.command.commands

val randomImageScrapingCommands = commands {
    command("suchar") {
        help(returns = "zdjęcie z sucharem")

        files { event ->
            val response = client.get("https://www.suchary.com/random.html").bodyAsText()

            val url = htmlDocument(response) {
                findFirst(".file-container a img") {
                    attribute("src")
                }
            }

            listOf(url)
        }
    }

    command("frog", "zabka", "żabka", "zaba", "żaba") {
        help(returns = "zdjęcie z żabką")

        val baseUrl = "https://generatorfun.com"

        files {
            val response = client.get("$baseUrl/random-frog-image").bodyAsText()

            val url = htmlDocument(response) {
                findFirst(".main-template img") {
                    attribute("src")
                }
            }

            listOf(if (url.startsWith("http")) url else "$baseUrl/$url")
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
