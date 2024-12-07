native-image \
  -H:+AllowIncompleteClasspath \
  --initialize-at-build-time=io.netty.util.internal.logging,io.netty.handler.codec,scala.runtime.Statics$VM \
  --initialize-at-run-time=io.netty.buffer,io.netty.channel.epoll,io.netty.channel.kqueue,io.netty.handler.ssl,io.netty.internal.tcnative \
  --enable-all-security-services \
  --enable-http \
  --no-fallback \
  --enable-https \
  -jar scala-app.jar scala-app
