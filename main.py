import asyncio

from WiertarBot import WiertarBot

loop = asyncio.get_event_loop()

bot = WiertarBot(loop)

loop.run_forever()
