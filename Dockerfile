FROM openjdk:20
WORKDIR /opt/SuperTurtyBot/
RUN sudo apt-get update && sudo apt-get install -y ffmpeg
COPY build/libs/SuperTurtyBot-all.jar SuperTurtyBot.jar
CMD ["java", "-jar", "SuperTurtyBot.jar", "-env", "/env/.env"]
