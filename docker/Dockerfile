FROM ibm-semeru-runtimes:open-17-jdk-jammy as jdk

WORKDIR /app

# setup - jdk + libraries + python stuff + new user
RUN apt -y update && \
    apt install -y python3-minimal python3-pip && \
    apt clean -y && \
    useradd -U user && \
    pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir poetry requests wheel && \
    poetry config virtualenvs.create false

# dependencies
COPY poetry.lock pyproject.toml ./
COPY ./fbchat-asyncio/pyproject.toml ./fbchat-asyncio/poetry.lock ./fbchat-asyncio/

RUN poetry install --no-root --no-cache --without local \
    && poetry install -C fbchat-asyncio --no-root --no-cache \
    && ln -s "$(find / -wholename */libjep.so 2>/dev/null)" /usr/lib/libjep.so

ENV JEP_LIB_PATH=/usr/lib/libjep.so

# actual stuff
COPY ./docker/init.sh ./
COPY ./WiertarBot/ ./WiertarBot/
COPY ./fbchat-asyncio/fbchat/ ./fbchat-asyncio/fbchat/
COPY ./data ./static-data
COPY build/libs/*.jar app.jar

RUN poetry install --no-cache --only-root  \
    && poetry install --no-cache -C fbchat-asyncio --only-root  \
    && chown -R user:user /app \
    && chmod +x ./init.sh ./app.jar

USER user

CMD [ "/app/init.sh" ]
