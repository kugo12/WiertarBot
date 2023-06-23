FROM ibm-semeru-runtimes:open-17-jdk-jammy as jdk

WORKDIR /app

RUN apt update \
    && apt install -y ffmpeg \
    && rm -rf /var/lib/apt/lists/* && apt clean -y \
    && useradd -U user

COPY build/libs/*.jar app.jar

RUN chmod +x ./app.jar

USER user

CMD [ "/app/app.jar" ]
