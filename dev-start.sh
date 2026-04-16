#!/bin/bash

set -e

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Starting dev database..."
cd "$ROOT_DIR/docker"
docker-compose up -d db

echo "Waiting briefly for database..."
sleep 3

echo "Starting backend (Quarkus dev)..."
cd "$ROOT_DIR/backend"
gnome-terminal -- bash -c "./mvnw quarkus:dev; exec bash" 2>/dev/null ||
  x-terminal-emulator -e bash -c "./mvnw quarkus:dev; exec bash" 2>/dev/null ||
  ./mvnw quarkus:dev &

echo "Waiting briefly for backend..."
sleep 5

echo "Starting frontend (Vite dev)..."
cd "$ROOT_DIR/frontend"
gnome-terminal -- bash -c "npm run dev; exec bash" 2>/dev/null ||
  x-terminal-emulator -e bash -c "npm run dev; exec bash" 2>/dev/null ||
  npm run dev &

echo "Dev stack started."
echo "Frontend: http://localhost:5173"
echo "Backend:  http://localhost:8080"
