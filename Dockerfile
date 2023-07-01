FROM openjdk:18-alpine
WORKDIR /opt/SuperTurtyBot/
COPY build/libs/SuperTurtyBot.jar /opt/SuperTurtyBot/build/libs/SuperTurtyBot.jar
CMD ["java", "-jar", "/opt/SuperTurtyBot/build/libs/SuperTurtyBot.jar", "--nogui", "--env=/env/.env"]