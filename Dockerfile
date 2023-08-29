FROM ubuntu:20.04 AS ffmpeg-installer
RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg

# Final stage with OpenJDK 20 and copied FFmpeg
FROM openjdk:20

WORKDIR /opt/SuperTurtyBot/

COPY --from=ffmpeg-installer /usr/bin/ffmpeg /usr/bin/ffmpeg
# Add FFmpeg to the PATH
ENV PATH="/usr/bin/ffmpeg:${PATH}"

COPY build/libs/SuperTurtyBot-all.jar SuperTurtyBot.jar

CMD ["java", "-jar", "SuperTurtyBot.jar", "-env", "/env/.env"]
