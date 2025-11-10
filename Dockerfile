FROM eclipse-temurin:21-jdk
RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg && \
    rm -rf /var/lib/apt/lists/*
WORKDIR /opt/SuperTurtyBot/
COPY build/libs/SuperTurtyBot-all.jar SuperTurtyBot.jar
CMD ["java", "-jar", "SuperTurtyBot.jar", "-env", "/env/.env"]
