#!/bin/bash
set -e  # Прерывать выполнение при любой ошибке

function cleanup() {
    echo "Cleaning up running containers..."
    docker rm -f crac-checkpoint 2>/dev/null || true
}
trap cleanup EXIT

echo "Init CRaC"

sbt "update;assembly"
mkdir -p target/cr && chmod +x target/cr

# Функция с ретраем до 3 раз
function retry_build_checkpoint() {
    local attempts=3
    local count=0
    until docker build -f Dockerfile.checkpoint -t crac-checkpoint .; do
        ((count++))
        if [ "$count" -ge "$attempts" ]; then
            echo "Build failed after $attempts attempts."
            return 1
        fi
        echo "Build failed... retrying ($count/$attempts)"
    done
}

retry_build_checkpoint

echo "Start CRaC snapshot app"
docker run -d --rm -v "$(pwd)/target/cr:/cr" \
    --cap-add=CHECKPOINT_RESTORE \
    --cap-add=SYS_PTRACE \
    -p 8084:8084 \
    -e APP_LABEL=crac \
    -e APP_PORT=8084 \
    --name crac-checkpoint \
    crac-checkpoint

echo "Waiting 5s"
sleep 5

echo "Warmup"
for i in {1..10000}; do
    bash request-example.sh -q || {
        echo "request-example.sh failed at iteration $i"
        exit 1
    }
done

echo "Take snapshot"
docker exec -it crac-checkpoint jcmd crac JDK.checkpoint
sleep 5


echo "Build CRaC Restore"
docker build -f Dockerfile.restore -t scala-app-crac .
