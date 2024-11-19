default:
  @just --list

# run a docker compose  
run args="":
    docker-compose up

run-exact args="":
    docker-compose up {{ args }}

run-rebuild args="":
    docker-compose up --build --force-recreate