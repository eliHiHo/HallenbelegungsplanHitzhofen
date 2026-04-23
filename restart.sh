#!/usr/bin/env bash
# restart.sh  — stoppt und startet den Dev-Stack neu

set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

"$ROOT/stop.sh"
echo ""
"$ROOT/start.sh"
