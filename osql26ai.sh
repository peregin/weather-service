#!/usr/bin/env bash

# PDBADMIN, SYSTEM, SYS users
CONTAINER_REPO="oracle26ai"
ORACLE_IMAGE="container-registry.oracle.com/database/free:23.26.0.0"
echo "checking $CONTAINER_REPO ..."
if [[ $(docker inspect -f '{{.State.Running}}' $CONTAINER_REPO) = "true" ]]; then
  echo "$CONTAINER_REPO is already running ..."
else
  echo home directory is "$HOME"
  docker run -d -p 1522:1521 -p 5500:5500 \
      --rm --name $CONTAINER_REPO \
      --security-opt label=disable \
      --shm-size=2g \
      -e ORACLE_PWD=password \
      "$ORACLE_IMAGE"
  echo "$CONTAINER_REPO has been started ..."
fi
