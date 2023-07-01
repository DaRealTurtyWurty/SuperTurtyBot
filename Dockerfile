FROM gradle:7.4-jdk18 as builder
WORKDIR /home/gradle/source/

COPY settings.gradle ./
COPY build.gradle ./
COPY src/ src/
RUN gradle installDist
RUN ls -Rla
FROM openjdk:18
WORKDIR /opt/SuperTurtyBot/
COPY --from=builder /home/gradle/source/build/install/SuperTurtyBot/ ./
RUN gradlew shadowJar
MOV ./build/libs/SuperTurtyBot-all.jar ./SuperTurtyBot.jar \
    && rm -rf ./build \
    && rm -rf ./gradle \
    && rm -rf ./gradlew \
    && rm -rf ./gradlew.bat \
    && rm -rf ./settings.gradle \
    && rm -rf ./build.gradle \
    && rm -rf ./src \
    && rm -rf ./gradle.properties \
    && rm -rf ./gradle-wrapper.jar \
    && rm -rf ./gradle-wrapper.properties \
    && rm -rf ./gradlew \
    && rm -rf ./gradlew.bat \
    && rm -rf ./settings.gradle

CMD ["java", "-jar", "SuperTurtyBot.jar", "--nogui", "--env=/env/.env"]