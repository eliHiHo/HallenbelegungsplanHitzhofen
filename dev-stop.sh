#!/bin/bash

set +e

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Stopping frontend..."
pkill -f "vite" || true
pkill -f "npm run dev" || true

echo "Stopping backend..."
pkill -f "quarkus:dev" || true
pkill -f "java.*quarkus" || true

echo "Stopping database..."
cd "$ROOT_DIR/docker"
docker-compose stop db

echo "Dev stack stopped."
