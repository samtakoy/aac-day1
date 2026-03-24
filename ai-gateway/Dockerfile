FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY ai-gateway/build/libs/ai-gateway.jar app.jar
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]
