#!/usr/bin/env bash
# stop.sh  — beendet Backend (Quarkus) und Frontend (Vite)

set -euo pipefail

log() { echo "[$(date +%H:%M:%S)] $*"; }

log "Stoppe Backend …"
pkill -f "quarkus:dev"     2>/dev/null && log "  ✓ Maven-Wrapper gestoppt"  || true
pkill -f "backend-dev.jar" 2>/dev/null && log "  ✓ Quarkus-JVM gestoppt"    || true

OLD=$(lsof -ti :8080 2>/dev/null || true)
[[ -n "$OLD" ]] && { log "  Port 8080 noch belegt (PID $OLD) — beende …"; kill "$OLD" 2>/dev/null || true; } || true

log "Stoppe Frontend …"
pkill -f "vite"        2>/dev/null && log "  ✓ Vite gestoppt"        || true
pkill -f "npm run dev" 2>/dev/null && log "  ✓ npm run dev gestoppt" || true

log ""
log "✓ Dev-Stack gestoppt."
