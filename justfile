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
