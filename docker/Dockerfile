FROM pypy:3.9-slim

WORKDIR /app

RUN apt -y update &&  \
    apt install -y libxml2-dev libxslt-dev zlib1g-dev libjpeg-dev gcc libfreetype6-dev libpq-dev && \
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
