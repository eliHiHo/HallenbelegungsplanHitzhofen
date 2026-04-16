#!/bin/bash

set -e

echo "🚀 Starting production stack..."

cd "$(dirname "$0")"

# optional: pull latest images
docker-compose pull

# build + start
docker-compose up -d --build

echo "✅ Containers started"

echo "📦 Status:"
docker-compose ps

echo "📜 Backend logs (last 20 lines):"
docker-compose logs --tail=20 backend
