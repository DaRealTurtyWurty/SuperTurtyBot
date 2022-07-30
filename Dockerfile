FROM gradle:7.5-jdk17 as builder
WORKDIR /home/gradle/source/

COPY build.gradle ./
COPY src/ src/
RUN gradle installDist
RUN ls -Rla

FROM eclipse-temurin:17-jre
WORKDIR /opt/SuperTurtyBot/

COPY --from=builder /home/gradle/source/build/install/SuperTurtyBot/ ./
COPY run ./

ENTRYPOINT ["./run"]
