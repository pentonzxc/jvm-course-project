# Use a base image with sbt and JDK
FROM sbtscala/scala-sbt:eclipse-temurin-18.0.2_1.8.0_3.2.0

# Set working directory
WORKDIR /app

# Copy the sbt project files
COPY build.sbt /app/
COPY project /app/project

# Fetch sbt dependencies
RUN sbt update

# Copy the source code
COPY src /app/src

# Build the assembly JAR
RUN sbt assembly

# Use a minimal base image for the runtime
FROM openjdk:18.0.2.1-slim-buster

# Set working directory
WORKDIR /app

# Copy the assembled JAR from the build stage
COPY --from=0 /app/scala-app.jar app.jar

# Define the entry point
ENTRYPOINT ["java", "-jar", "app.jar"]