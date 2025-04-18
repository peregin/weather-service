#!/usr/bin/env bash

# PDBADMIN, SYSTEM, SYS users
CONTAINER_REPO="oracle23ai"
echo "checking $CONTAINER_REPO ..."
if [[ $(docker inspect -f '{{.State.Running}}' $CONTAINER_REPO) = "true" ]]; then
  echo "$CONTAINER_REPO is already running ..."
else
  echo home directory is "$HOME"
  docker run -d -p 1521:1521 -p 5500:5500 \
      --rm --name $CONTAINER_REPO \
      --security-opt label=disable \
      --shm-size=2g \
      -e ORACLE_PWD=password \
      -v /opt/oracle/oradata23:/opt/oracle/oradata \
      container-registry.oracle.com/database/free:latest
  echo "$CONTAINER_REPO has been started ..."
fi
