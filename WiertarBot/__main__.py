import asyncio
import atexit

from .bot import WiertarBot
from .config import config
from .log import log
from .integrations.health import startup_complete


async def main() -> None:
    log.info("Starting initialization...")

    await config.init()
    bot = await WiertarBot.create()

    atexit.register(bot.save_cookies)
    asyncio.get_running_loop().create_task(
        WiertarBot.message_garbage_collector()
    )

    startup_complete()

    log.info("Initialization complete")

    await bot.run()

asyncio.run(main())
