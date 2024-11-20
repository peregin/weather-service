#!/usr/bin/env bash

set -e

# docker build triggers a production install with cargo
docker build -t peregin/velocorner.weather .
docker push peregin/velocorner.weather:latest

echo "Successfully deployed..."

