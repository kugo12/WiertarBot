FROM pypy:3.7-slim-buster

WORKDIR /app

RUN apt-get -y update \
 && apt-get install -y chromium zlib1g-dev libjpeg-dev chromium-driver gcc libfreetype6-dev \
 && ln -s /usr/bin/chromium /usr/bin/google-chrome

COPY requirements.txt ./

RUN pip install --no-cache-dir -r requirements.txt

CMD [ "pypy3", "./main.py" ]
