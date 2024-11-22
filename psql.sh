#!/usr/bin/env bash

CONTAINER_REPO="weather_repo"
echo "checking $CONTAINER_REPO ..."
if [[ $(docker inspect -f '{{.State.Running}}' $CONTAINER_REPO) = "true" ]]; then
  echo "$CONTAINER_REPO is already running ..."
else
  echo home directory is "$HOME"
  docker run -d -p 5494:5432 \
    --rm --name $CONTAINER_REPO \
    --health-cmd='stat /etc/passwd || exit 1' \
    --health-interval=30s \
    -e POSTGRES_DB=weather \
    -e POSTGRES_USER=weather \
    -e POSTGRES_PASSWORD=weather \
    -v "$HOME"/Downloads/psql/weather/data:/var/lib/postgresql/data \
    postgres:16.4-alpine
  echo "$CONTAINER_REPO has been started ..."
fi
