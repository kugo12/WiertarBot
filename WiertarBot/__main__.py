import asyncio

from .connectors import FBConnector
from .config import config
from .log import log
from .integrations.health import startup_complete


async def main() -> None:
    log.info("Starting initialization...")

    await config.init()
    connector = await FBConnector.create()

    startup_complete()

    log.info("Initialization complete")

    await connector.run()

asyncio.run(main())
