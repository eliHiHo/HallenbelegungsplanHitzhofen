#!/bin/bash

set -e

echo "🛑 Stopping production stack..."

cd "$(dirname "$0")"

docker-compose down

echo "✅ All containers stopped"
