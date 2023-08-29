FROM ubuntu:latest
RUN sudo apt update && sudo apt upgrade
RUN sudo apt install ffmpeg
RUN ffmpeg -version

FROM openjdk:20
WORKDIR /opt/SuperTurtyBot/
COPY build/libs/SuperTurtyBot-all.jar SuperTurtyBot.jar
CMD ["java", "-jar", "SuperTurtyBot.jar", "-env", "/env/.env"]
