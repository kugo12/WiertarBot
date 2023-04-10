FROM python:3.11-slim

WORKDIR /app

RUN useradd -U user && \
    pip install --no-cache-dir poetry wheel && \
    poetry config virtualenvs.create false

# dependencies
COPY poetry.lock pyproject.toml ./

RUN poetry install --no-cache

COPY ./src/ ./src/
USER user

ENV GUNICORN_CMD_ARGS="-w 1 --preload -k gthread -t 10 -b 0.0.0.0:8080"

CMD [ "gunicorn", "src:app" ]
