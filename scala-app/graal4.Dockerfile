FROM --platform=linux/amd64 graal-sbt-builder

# RUN java --version
# RUN native-image --help
# RUN gu --help

WORKDIR /app

# Copy the sbt project files
COPY build.sbt /app/
COPY project /app/project
COPY src /app/src

RUN ls

# Fetch sbt dependencies
RUN sbt "update" assembly

RUN sbt "show GraalVMNativeImage / packageBin"

# Build the assembly JAR

ENTRYPOINT ["target/graalvm-native-image/scala-course-project"]
