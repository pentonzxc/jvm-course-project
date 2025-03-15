FROM ghcr.io/graalvm/native-image-community:23.0.2-muslib-ol8-20250121

RUN microdnf install -y \
	git \
	tar \
	curl \
	unzip \
	&& microdnf clean all

# idk, this doesn't fix a problem
RUN curl -o /etc/pki/ca-trust/source/anchors/mozilla-ca.pem https://curl.se/ca/cacert.pem && \
	update-ca-trust extract

# Install SBT
RUN curl --insecure -L -o sbt.tgz https://github.com/sbt/sbt/releases/download/v1.10.6/sbt-1.10.6.tgz && \
	tar -xvzf sbt.tgz -C /usr/local && \
	rm sbt.tgz && \
	ln -s /usr/local/sbt/bin/sbt /usr/local/bin/sbt
