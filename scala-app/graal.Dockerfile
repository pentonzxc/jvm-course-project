FROM --platform=linux/amd64 ghcr.io/graalvm/native-image-community:23.0.2-muslib-ol8-20250121

RUN microdnf install -y \
	git \
	tar \
	curl \
	unzip \
	&& microdnf clean all

# try to fix --insecure problem, not help
# RUN curl -o /etc/pki/ca-trust/source/anchors/mozilla-ca.pem https://curl.se/ca/cacert.pem && \
# 	update-ca-trust extract

# Install SBT
RUN curl --insecure -L -o sbt.tgz https://github.com/sbt/sbt/releases/download/v1.10.6/sbt-1.10.6.tgz && \
	tar -xvzf sbt.tgz -C /usr/local && \
	rm sbt.tgz && \
	ln -s /usr/local/sbt/bin/sbt /usr/local/bin/sbt

WORKDIR /app
# copy sources
COPY build.sbt /app/
COPY project /app/project
COPY src /app/src

RUN ls

RUN sbt ";update;show GraalVMNativeImage / packageBin" 

ENTRYPOINT ["target/graalvm-native-image/scala-course-project"]



