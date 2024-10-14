FROM openjdk:21-slim-buster as builder

WORKDIR /opt/SuperTurtyBot/
COPY build/libs/SuperTurtyBot-all.jar SuperTurtyBot.jar

FROM debian:bullseye-slim
WORKDIR /opt/SuperTurtyBot/

RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg && \
    rm -rf /var/lib/apt/lists/*

COPY --from=builder /opt/SuperTurtyBot/SuperTurtyBot.jar .

CMD ["java", "-jar", "SuperTurtyBot.jar", "-env", "/env/.env"]
