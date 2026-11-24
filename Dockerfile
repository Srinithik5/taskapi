# ---------- Build stage: full JDK + Maven, used only to compile the jar ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy just the pom first so Docker can cache the downloaded dependencies in their own
# layer - they only get re-downloaded if pom.xml itself changes, not on every code edit.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Runtime stage: JRE only, no Maven, no source code ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/taskapi-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
