# Use a base image with sbt and JDK
FROM sbtscala/scala-sbt:graalvm-ce-22.3.3-b1-java17_1.10.6_2.13.15

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
FROM eclipse-temurin:17.0.13_11-jdk-noble

# Set working directory
WORKDIR /app

# Copy the assembled JAR from the build stage
COPY --from=0 /app/scala-app.jar app.jar

# Define the entry point
ENTRYPOINT ["java", "-jar", "app.jar"]
