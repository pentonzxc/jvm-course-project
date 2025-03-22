default:
  @just --list

# run a docker compose  
run args="":
    docker-compose up -d

stop args="":
    docker-compose down

run-exact args="":
    docker-compose up {{ args }}

run-rebuild args="":
    docker-compose up --build --force-recreate

watch-logs args="":
  while true; do sleep 2; docker-compose logs {{ args }} -f; done

run-go-and-graal args="":
    docker-compose up scala-app-native go-app scala-app-client go-app-lciet

[no-cd]
helm-run args="":
    helm install course-project . -f values.yaml {{ args }}


[no-cd]
helm-stop:
    helm uninstall course-project

pods-get:
    kubectl get pods
