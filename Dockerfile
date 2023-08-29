FROM ubuntu:20.04

# Install FFmpeg, OpenJDK, and other necessary packages
RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg openjdk-20-jdk && \
    apt-get clean
	
WORKDIR /opt/SuperTurtyBot/

COPY build/libs/SuperTurtyBot-all.jar SuperTurtyBot.jar
CMD ["java", "-jar", "SuperTurtyBot.jar", "-env", "/env/.env"]
