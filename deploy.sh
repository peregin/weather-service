#!/usr/bin/env bash

# Strict error handling
set -euo pipefail
IFS=$'\n\t'

# Configuration
BUILDER_NAME="multi-arch-builder"
IMAGE_NAME="peregin/velocorner.weather"
TAG="latest"
PLATFORMS="linux/arm64"
# read it as parameter of default it
DOCKERFILE=${1:-"Dockerfile"}
echo "building from $DOCKERFILE"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logger function
log() {
    local level=$1
    shift
    local color
    case "$level" in
        "INFO") color="$GREEN";;
        "WARN") color="$YELLOW";;
        "ERROR") color="$RED";;
        *) color="$NC";;
    esac
    echo -e "${color}[$(date +'%Y-%m-%d %H:%M:%S')] [$level] $*${NC}"
}

# Error handler
error_handler() {
    log "ERROR" "An error occurred on line $1"
    exit 1
}

# Set up error handling
trap 'error_handler ${LINENO}' ERR

# Check Docker is running
if ! docker info >/dev/null 2>&1; then
    log "ERROR" "Docker is not running or not accessible"
    exit 1
fi

# Initialize buildx
init_buildx() {
    if ! docker buildx inspect "${BUILDER_NAME}" >/dev/null 2>&1; then
        log "INFO" "Creating new buildx instance: ${BUILDER_NAME}"
        docker buildx create --name "${BUILDER_NAME}" --driver docker-container --bootstrap
    else
        log "INFO" "Using existing buildx instance: ${BUILDER_NAME}"
    fi
    docker buildx use "${BUILDER_NAME}"
}

# Build and push image
build_and_push() {
    log "INFO" "Building multi-arch image for platforms: ${PLATFORMS}"
    docker buildx build \
        --platform "${PLATFORMS}" \
        --tag "${IMAGE_NAME}:${TAG}" \
        --push \
        --cache-from "type=registry,ref=${IMAGE_NAME}:${TAG}" \
        --cache-to "type=inline" \
        --progress=plain \
        --file "${DOCKERFILE}" \
        .
}

# Main execution
main() {
    log "INFO" "Starting deployment process..."

    init_buildx
    build_and_push

    log "INFO" "Deployment completed successfully!"
}

# Execute main function
main
