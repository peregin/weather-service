#!/usr/bin/env bash

CONTAINER_REPO="oracle19.3"
echo "checking $CONTAINER_REPO ..."
if [[ $(docker inspect -f '{{.State.Running}}' $CONTAINER_REPO) = "true" ]]; then
  echo "$CONTAINER_REPO is already running ..."
else
  echo home directory is "$HOME"
  docker run -d -p 1521:1521 -p 5500:5500 \
      --rm --name $CONTAINER_REPO \
      --health-cmd='stat /etc/passwd || exit 1' \
      --health-interval=30s \
      -e ORACLE_PDB=orapdb1 \
      -e ORACLE_PWD=password \
      -e ORACLE_MEM=3000 \
      -v /opt/oracle/oradata:/opt/oracle/oradata \
      -d oracle/database:19.3.0-ee
  echo "$CONTAINER_REPO has been started ..."
fi
