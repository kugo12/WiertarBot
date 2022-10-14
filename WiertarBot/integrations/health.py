from typing import Callable, Union, Awaitable

from aiohttp import web
from inspect import isawaitable
from http import HTTPStatus

from ..config import config, HealthConfig
from ..log import log

Probe = Callable[[], Union[bool, Awaitable[bool]]]

_liveness_probes: list[Probe] = []
_readiness_probes: list[Probe] = []
_startup_complete = False


@config.register_init
@config.on(HealthConfig)
async def _init(config: HealthConfig) -> None:
    log.info("Starting health server...")

    app = web.Application()
    app.router.add_routes(
        [
            web.get("/health/live", _liveness),
            web.get("/health/ready", _readiness),
            web.get("/health/startup", _startup),
        ]
    )

    kwargs = {} if config.access_log else {"access_log": None}
    runner = web.AppRunner(app, **kwargs)
    await runner.setup()

    site = web.TCPSite(runner, config.host, config.port)
    await site.start()

    log.info("Started health server")


async def _evaluate_probes(probes: list[Probe]) -> bool:
    results: list[bool] = []
    for probe in probes:
        result = probe()
        results.append(await result if isawaitable(result) else result)

    return all(results)


def _probe_response(status: bool) -> web.Response:
    return web.Response(text="", status=HTTPStatus.OK if status else HTTPStatus.SERVICE_UNAVAILABLE)


async def _liveness(_: web.Request) -> web.Response:
    return _probe_response(await _evaluate_probes(_liveness_probes))


async def _readiness(_: web.Request) -> web.Response:
    return _probe_response(await _evaluate_probes(_readiness_probes))


async def _startup(_: web.Request) -> web.Response:
    return _probe_response(_startup_complete)


def liveness_probe(func: Probe) -> Probe:
    _liveness_probes.append(func)
    return func


def readiness_probe(func: Probe) -> Probe:
    _readiness_probes.append(func)
    return func


def startup_complete() -> None:
    global _startup_complete
    _startup_complete = True
