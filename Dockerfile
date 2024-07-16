FROM gradle:8.5.0-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon 

FROM amazoncorretto:17.0.11

EXPOSE 8080

RUN mkdir /app

COPY --from=build /home/gradle/src/build/libs/*.jar /app/

ENTRYPOINT ["java","-jar","/app/SpringReactiveWS-1.0-SNAPSHOT.jar", "--logging.level.de.sebampuerom=DEBUG"]