import asyncio

from WiertarBot import WiertarBot
from WiertarBot.integrations import sentry

sentry.init()
bot = WiertarBot()
asyncio.get_event_loop().run_forever()
