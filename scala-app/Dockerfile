FROM openjdk:21-jdk-slim

COPY scala-course-project-assembly-0.1.0-SNAPSHOT.jar scala-vertx.jar

RUN apt-get update && apt-get install -y curl

ENTRYPOINT java -jar scala-vertx.jar
