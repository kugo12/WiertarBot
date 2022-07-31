import asyncio

from .bot import WiertarBot
from .integrations import sentry

sentry.init()
bot = WiertarBot()
asyncio.get_event_loop().run_forever()
