FROM gradle:8.5-jdk17 AS build

WORKDIR /home/gradle/project

COPY --chown=gradle:gradle gradle ./gradle
COPY --chown=gradle:gradle *.gradle.kts settings.gradle.kts gradle.properties ./
COPY --chown=gradle:gradle mcp-server ./mcp-server

RUN gradle :mcp-server:build --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /home/gradle/project/mcp-server/build/libs/mcp-server.jar ./mcp-server.jar

EXPOSE 3000

ENV JAVA_OPTS="-Xmx256m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar mcp-server.jar"]
