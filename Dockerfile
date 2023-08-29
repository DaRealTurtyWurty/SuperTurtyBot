FROM ubuntu:20.04 AS ffmpeg-installer

RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg && \
    apt-get clean

# Final stage with OpenJDK 20 and copied FFmpeg
FROM openjdk:20

WORKDIR /opt/SuperTurtyBot/

# Copy the FFmpeg binary and it's libraries
COPY --from=ffmpeg-installer /usr/bin/ffmpeg /usr/bin/ffmpeg
COPY --from=ffmpeg-installer /usr/lib/x86_64-linux-gnu/libavdevice.so.58 /usr/lib/x86_64-linux-gnu/libavdevice.so.58

# Add FFmpeg to the PATH
ENV PATH="/usr/bin/ffmpeg:${PATH}"

COPY build/libs/SuperTurtyBot-all.jar SuperTurtyBot.jar

CMD ["java", "-jar", "SuperTurtyBot.jar", "-env", "/env/.env"]
