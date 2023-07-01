FROM openjdk:18-alpine
WORKDIR /opt/SuperTurtyBot/
COPY build/libs/SuperTurtyBot-all.jar ./SuperTurtyBot.jar
CMD ["java", "-jar", "./build/libs/SuperTurtyBot.jar", "--nogui", "--env=/env/.env"]