# --- build stage: compile the project and produce the fat JAR ---
FROM maven:3-openjdk-17-slim AS build
WORKDIR /workspace

# copy only what is needed to leverage Docker layer cache for dependencies
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN mvn -B -f pom.xml -N dependency:go-offline

# copy source and build
COPY src ./src
RUN mvn -B -DskipTests package

# --- runtime stage: minimal image to run the JAR as non-root ---
FROM openjdk:17-jdk-slim
ARG APP_JAR_NAME
WORKDIR /app

# create non-root user
RUN useradd --create-home --shell /bin/bash appuser \
  && mkdir /app/logs \
  && chown -R appuser:appuser /app

# copy jar from build stage (matches target/*.jar)
COPY --from=build /workspace/target/*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar

USER appuser
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
