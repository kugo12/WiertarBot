FROM python:3.11-slim

WORKDIR /app

# setup - jdk + libraries + python stuff + new user
RUN apt -y update && \
    apt install -y wget gnupg libxml2-dev libxslt-dev zlib1g-dev gcc && \
    wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | apt-key add - && \
    echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list && \
    apt -y update && \
    apt install -y temurin-17-jdk && \
    apt clean -y && \
    useradd -U user && \
    pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir poetry requests wheel && \
    poetry config virtualenvs.create false

# dependencies
COPY poetry.lock pyproject.toml ./
COPY ./fbchat-asyncio/pyproject.toml ./fbchat-asyncio/poetry.lock ./fbchat-asyncio/

RUN export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")  \
    && poetry install --no-root --no-cache --without local \
    && poetry install -C fbchat-asyncio --no-root --no-cache \
    && JEP_LIB_PATH="$(find / -wholename *site-packages/jep 2>/dev/null)/libjep.so" \
    && ln -s "$JEP_LIB_PATH" /usr/lib/libjep.so

# actual stuff
COPY ./docker/init.sh ./
COPY ./WiertarBot/ ./WiertarBot/
COPY ./fbchat-asyncio/fbchat/ ./fbchat-asyncio/fbchat/
COPY ./data ./static-data
COPY build/libs/*.jar app.jar

RUN poetry install --no-cache --only-root  \
    && poetry install -C fbchat-asyncio --only-root  \
    && chown -R user:user /app \
    && chmod +x ./init.sh ./app.jar

USER user

CMD [ "/app/init.sh" ]
