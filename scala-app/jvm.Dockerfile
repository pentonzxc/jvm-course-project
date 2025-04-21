FROM --platform=linux/amd64 openjdk:23-jdk-slim

RUN apt-get update && apt-get install -y curl


# Install SBT
RUN curl -L -o sbt.tgz https://github.com/sbt/sbt/releases/download/v1.10.6/sbt-1.10.6.tgz && \
	tar -xvzf sbt.tgz -C /usr/local && \
	rm sbt.tgz && \
	ln -s /usr/local/sbt/bin/sbt /usr/local/bin/sbt

WORKDIR /app

# copy sources
COPY build.sbt /app/
COPY project /app/project
COPY src /app/src
COPY lib /app/lib

RUN sbt ";update;show assembly" 

ENTRYPOINT ["java", "-jar", "/app/target/scala-app.jar"]



