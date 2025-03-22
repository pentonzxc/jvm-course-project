echo "Build scala-app native"
sbt ";update;show assembly" 
native-image -jar ./target/scala-app.jar scala-app-native --verbose
