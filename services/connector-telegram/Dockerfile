FROM ibm-semeru-runtimes:open-17-jdk-jammy as jdk

WORKDIR /app

COPY build/libs/*.jar app.jar

RUN useradd -U user && \
    chown -R user:user /app

USER user

CMD [ "/app/app.jar" ]
