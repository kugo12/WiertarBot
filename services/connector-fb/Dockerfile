FROM ibm-semeru-runtimes:open-25-jre-jammy as jdk

WORKDIR /app

COPY build/libs/*.jar app.jar

RUN useradd -U user && \
    chown -R user:user /app

USER user

CMD [ "java", "-jar", "/app/app.jar" ]
