FROM openjdk:21-jdk-slim

COPY scala-course-project-assembly-0.1.0-SNAPSHOT.jar scala-vertx.jar

ENTRYPOINT java -jar scala-vertx.jar
