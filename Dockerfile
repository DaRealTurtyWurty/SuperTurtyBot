FROM gradle:7.5-jdk17 as builder
WORKDIR /opt/SuperTurtyBot/code/

COPY build.gradle ./
COPY src/ src/
RUN gradle installDist

FROM eclipse-temurin:17-jre
WORKDIR /opt/SuperTurtyBot/

COPY --from=builder /opt/SuperTurtyBot/code/build/install/SuperTurtyBot/ ./
COPY run ./

ENTRYPOINT ["./run"]
