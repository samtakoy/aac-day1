FROM gradle:8.5-jdk17 AS build

WORKDIR /home/gradle/project

COPY --chown=gradle:gradle gradle ./gradle
COPY --chown=gradle:gradle *.gradle.kts settings.gradle.kts gradle.properties ./
COPY --chown=gradle:gradle rag-server ./rag-server

RUN gradle :rag-server:build --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /home/gradle/project/rag-server/build/libs/rag-server.jar ./rag-server.jar

EXPOSE 3001

ENV JAVA_OPTS="-Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar rag-server.jar"]
