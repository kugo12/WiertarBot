FROM python:3.14-alpine

WORKDIR /app

RUN addgroup app && \
        adduser -s /sbin/nologin -DH -G app app && \
    pip install --no-cache-dir poetry wheel && \
    poetry config virtualenvs.create false

COPY poetry.lock pyproject.toml ./

RUN poetry install --no-cache

COPY ./main.py .
USER app:app

CMD [ "python", "main.py" ]
