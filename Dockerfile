FROM eclipse-temurin:21
ENV JAVA_HOME=/opt/java/openjdk
COPY --from=eclipse-temurin:21 $JAVA_HOME $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"
RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg && \
    rm -rf /var/lib/apt/lists/*
WORKDIR /opt/SuperTurtyBot/
COPY build/libs/SuperTurtyBot-all.jar SuperTurtyBot.jar
CMD ["java", "-jar", "SuperTurtyBot.jar", "-env", "/env/.env"]