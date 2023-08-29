FROM ubuntu:latest
RUN apt-get update && apt-get install -y ffmpeg

FROM openjdk:20
WORKDIR /opt/SuperTurtyBot/
COPY build/libs/SuperTurtyBot-all.jar SuperTurtyBot.jar
CMD ["java", "-jar", "SuperTurtyBot.jar", "-env", "/env/.env"]
