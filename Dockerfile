FROM amazoncorretto:17-alpine3.18-jdk

EXPOSE 8082

RUN mkdir -p /app/

COPY target/manager-0.0.1-SNAPSHOT.jar /app/manager.jar

ENTRYPOINT ["java", "-jar", "/app/manager.jar"]
