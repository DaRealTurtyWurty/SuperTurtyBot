FROM openjdk:21
WORKDIR /opt/SuperTurtyBot/
COPY build/libs/SuperTurtyBot-all.jar SuperTurtyBot.jar
RUN apk update && \
    apk upgrade && \
    apk add --no-cache ffmpeg
CMD ["java", "-jar", "SuperTurtyBot.jar", "-env", "/env/.env"]
