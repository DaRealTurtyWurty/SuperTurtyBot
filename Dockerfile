ARG JAVA_VERSION=21
FROM openjdk:$JAVA_VERSION as builder

COPY ./ /build/
WORKDIR /build
RUN chmod +x gradlew
RUN ./gradlew build

ARG JAVA_VERSION=21
FROM openjdk:$JAVA_VERSION
WORKDIR /opt/SuperTurtyBot
COPY --from=builder /build/build/libs/SuperTurtyBot-all.jar SuperTurtyBot.jar
RUN apt-get update; \
    apt-get upgrade -y; \
    apt-get install -y --no-install-recommends ffmpeg; \
    rm -rf /var/lib/apt/lists/*
ENTRYPOINT ["java", "-jar", "SuperTurtyBot.jar", "-env", "/env/.env"]
