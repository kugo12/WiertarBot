FROM pypy:3.9-slim

WORKDIR /app

RUN apt -y update &&  \
    apt install -y chromium-driver libxml2-dev libxslt-dev zlib1g-dev libjpeg-dev gcc libfreetype6-dev libpq-dev && \
    ln -s /usr/bin/chromium /usr/bin/google-chrome && \
    apt clean -y && \
    useradd -U user && \
    pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir requests wheel

COPY requirements.txt ./

RUN pip install --no-cache-dir -r requirements.txt

COPY ./WiertarBot ./WiertarBot
COPY ./tools ./tools
RUN chown -R user:user /app

USER user

CMD [ "pypy3", "-m", "WiertarBot" ]
