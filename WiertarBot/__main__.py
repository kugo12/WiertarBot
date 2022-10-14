import asyncio
import atexit

from .bot import WiertarBot
from .config import config


async def main() -> None:
    await config.init()
    # bot = await WiertarBot.create()

    # atexit.register(bot.save_cookies)
    # asyncio.get_running_loop().create_task(
    #     WiertarBot.message_garbage_collector()
    # )
    #
    # await bot.run()
    pass

asyncio.run(main())
