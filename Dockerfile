FROM ubuntu:20.04
RUN apt-get -y update && apt-get -y upgrade && apt-get install -y --no-install-recommends ffmpeg

FROM openjdk:20
WORKDIR /opt/SuperTurtyBot/
COPY build/libs/SuperTurtyBot-all.jar SuperTurtyBot.jar
CMD ["java", "-jar", "SuperTurtyBot.jar", "-env", "/env/.env"]
