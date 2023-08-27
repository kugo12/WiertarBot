FROM ibm-semeru-runtimes:open-17-jdk-jammy as jdk

WORKDIR /app

# actual stuff
COPY ./docker/init.sh ./
COPY ./data ./static-data
COPY build/libs/*.jar app.jar

RUN useradd -U user && \
    chown -R user:user /app && \
    chmod +x ./init.sh ./app.jar

USER user

CMD [ "/app/init.sh" ]
