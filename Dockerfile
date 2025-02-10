FROM amazoncorretto:17-alpine3.19-jdk

ARG TZ='America/Bogota'
ENV TZ=${TZ}
ENV APP_DIR /home

WORKDIR ${APP_DIR}

# Instalar tzdata para zonas horarias
RUN apk add --no-cache tzdata

# Configurar la zona horaria
RUN cp /usr/share/zoneinfo/${TZ} /etc/localtime && \
    echo ${TZ} > /etc/timezone

COPY target/manager-0.0.1-SNAPSHOT.jar $APP_DIR/manager.jar

CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-Duser.timezone=${TZ}", "-jar", "/home/manager.jar"]

EXPOSE 8082
