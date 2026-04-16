#!/bin/bash

set -e

echo "🔄 Restarting stack..."

./stop.sh
./start.sh
