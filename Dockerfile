FROM gradle:8.5.0-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon 

FROM amazoncorretto:17.0.11

EXPOSE 8080

RUN mkdir /app

COPY --from=build /home/gradle/src/build/libs/*.jar /app/

ENV API_URL=http://localhost:8000/prompt
ENV LOG_LEVEL=INFO

ENTRYPOINT ["java", "-jar", "/app/SpringReactiveWS-1.0-SNAPSHOT.jar"]
CMD ["--logging.level.de.sebampuerom=${LOG_LEVEL}", "--api.url=${API_URL}"]