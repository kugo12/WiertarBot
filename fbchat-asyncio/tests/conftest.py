import pytest
import fbchat


@pytest.fixture(scope="session")
def domain():
    return "messenger.com"


@pytest.fixture(scope="session")
def session(domain: str):
    return fbchat.Session(
        user_id="31415926536",
        fb_dtsg=None,
        revision=None,
        session=None,
        domain=domain
    )
