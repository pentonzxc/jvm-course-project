echo "Build scala-app native"
native-image -jar target/scala-app.jar scala-app-native --verbose
