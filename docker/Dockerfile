FROM ibm-semeru-runtimes:open-17-jdk-jammy as jdk

WORKDIR /app

# setup - jdk + libraries + python stuff + new user
RUN apt -y update && \
    apt install -y python3.11-minimal python3.11-dev gcc && \
    apt clean -y && \
    useradd -U user && \
    curl -sS https://bootstrap.pypa.io/get-pip.py | python3.11 && \
    pip install --no-cache-dir poetry requests wheel && \
    poetry config virtualenvs.create false

# dependencies
COPY poetry.lock pyproject.toml ./

RUN poetry install --no-root --no-cache --without local \
    && ln -s "$(find / -wholename */libjep.so 2>/dev/null)" /usr/lib/libjep.so

ENV JEP_LIB_PATH=/usr/lib/libjep.so

# actual stuff
COPY ./docker/init.sh ./
COPY ./WiertarBot/ ./WiertarBot/
COPY ./data ./static-data
COPY build/libs/*.jar app.jar

RUN poetry install --no-cache --only-root  \
    && chown -R user:user /app \
    && chmod +x ./init.sh ./app.jar

USER user

CMD [ "/app/init.sh" ]
