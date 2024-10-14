FROM eclipse-temurin:21-jdk
WORKDIR /opt/SuperTurtyBot/
COPY build/libs/SuperTurtyBot-all.jar SuperTurtyBot.jar
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends ffmpeg && \
    rm -rf /var/lib/apt/lists/*
CMD ["java", "-jar", "SuperTurtyBot.jar", "-env", "/env/.env"]
