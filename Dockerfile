FROM openjdk:18-alpine
WORKDIR /opt/SuperTurtyBot/
COPY /build/libs/SuperTurtyBot-all.jar /opt/SuperTurtyBot/build/libs/SuperTurtyBot.jar
CMD ["java", "-jar", "/build/libs/SuperTurtyBot.jar", "--nogui", "--env=/env/.env"]