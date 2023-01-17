FROM python:3.11-slim

WORKDIR /app

RUN apt -y update &&  \
    apt install -y wget gnupg libxml2-dev libxslt-dev zlib1g-dev libjpeg-dev gcc libfreetype6-dev libpq-dev && \
    wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | apt-key add - && \
    echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list && \
    apt -y update && \
    apt install -y temurin-17-jdk && \
    apt clean -y && \
    useradd -U user && \
    pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir requests wheel

COPY requirements.txt ./

RUN pip install --no-cache-dir -r requirements.txt

COPY ./WiertarBot ./WiertarBot
COPY ./docker/init.sh /usr/local/bin/init.sh
COPY ./data ./static-data

RUN chown -R user:user /app && \
    chmod +x /usr/local/bin/init.sh

USER user

CMD [ "init.sh" ]
