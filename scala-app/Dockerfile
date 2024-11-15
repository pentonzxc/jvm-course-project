FROM openjdk:21-jdk-slim

COPY scala-app.jar scala-app.jar

RUN apt-get update && apt-get install -y curl

ENTRYPOINT java -jar scala-app.jar
