# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /var/data/uploads && chown -R app:app /var/data
USER app
ENV APP_UPLOAD_ROOT_DIR=/var/data/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
