FROM ibm-semeru-runtimes:open-25-jre-jammy as jdk

WORKDIR /app

RUN apt update \
    && apt install -y ffmpeg \
    && rm -rf /var/lib/apt/lists/* && apt clean -y \
    && useradd -U user

COPY build/libs/*.jar app.jar

USER user

CMD [ "java", "-jar", "/app/app.jar" ]
