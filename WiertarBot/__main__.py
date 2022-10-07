import asyncio
import atexit

from .bot import WiertarBot
from .integrations import sentry


async def main():
    # bot = await WiertarBot.create()

    # atexit.register(bot.save_cookies)
    # asyncio.get_running_loop().create_task(
    #     WiertarBot.message_garbage_collector()
    # )
    #
    # await bot.run()
    pass


sentry.init()
asyncio.run(main())
