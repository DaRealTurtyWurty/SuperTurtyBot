FROM ubuntu:20.04  # Or any newer version

WORKDIR /opt/SuperTurtyBot/

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y openjdk-21-jre ffmpeg && \
    rm -rf /var/lib/apt/lists/*

COPY build/libs/SuperTurtyBot-all.jar SuperTurtyBot.jar

CMD ["java", "-jar", "SuperTurtyBot.jar", "-env", "/env/.env"]
