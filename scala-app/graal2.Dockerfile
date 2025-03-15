FROM sbtscala/scala-sbt:graalvm-community-22.0.1_1.10.10_2.13.16

# Set working directory
WORKDIR /app

# Copy the sbt project files
COPY build.sbt /app/
COPY project /app/project
COPY src /app/src

# Fetch sbt dependencies
RUN sbt update

# Build the assembly JAR
RUN sbt "show GraalVMNativeImage / packageBin"

ENTRYPOINT ["target/graalvm-native-image/scala-course-project"]
