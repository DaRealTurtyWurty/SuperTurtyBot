FROM openjdk:18-alpine
WORKDIR /opt/SuperTurtyBot
COPY SuperTurtyBot.jar /opt/SuperTurtyBot/SuperTurtyBot.jar
CMD ["java", "-jar", "SuperTurtyBot.jar", "--nogui", "--env=/env/.env"]
