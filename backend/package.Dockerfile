FROM ibm-semeru-runtimes:open-25-jre-jammy as jre

WORKDIR /app

COPY build/libs/*.jar app.jar

RUN groupadd -g 61234 user && \
    useradd -u 61234 -g user user && \
    chown -R user:user /app

USER user

CMD [ "java", "-jar", "/app/app.jar" ]
