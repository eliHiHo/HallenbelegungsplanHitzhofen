#!/usr/bin/env bash
# start.sh  — startet den Dev-Stack: Backend (Quarkus) + Frontend (Vite)
#
# Voraussetzung: PostgreSQL läuft lokal auf Port 5432 mit DB "hallen"
# (Host-Postgres, nicht Docker — Docker-Compose ist nur für den Prod-Stack)
#
# Logs:
#   /tmp/hallen-backend.log
#   /tmp/hallen-frontend.log
#
# Stoppen:  ./stop.sh
# Restart:  ./restart.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
BACKEND_LOG="/tmp/hallen-backend.log"
FRONTEND_LOG="/tmp/hallen-frontend.log"

# ─── Umgebungsvariablen aus docker/.env laden ─────────────────────────────────
ENV_FILE="$ROOT/docker/.env"
if [[ -f "$ENV_FILE" ]]; then
    set -a
    # shellcheck disable=SC1090
    . "$ENV_FILE"
    set +a
    echo "[$(date +%H:%M:%S)] Env geladen aus $ENV_FILE (MAIL_HOST=${MAIL_HOST:-<nicht gesetzt>}, MAIL_USERNAME=${MAIL_USERNAME:-<nicht gesetzt>})"
else
    echo "[$(date +%H:%M:%S)] WARNUNG: $ENV_FILE nicht gefunden — Mail-Versand funktioniert nicht" >&2
fi

# ─── Hilfsfunktionen ──────────────────────────────────────────────────────────

log()  { echo "[$(date +%H:%M:%S)] $*"; }
die()  { echo "FEHLER: $*" >&2; exit 1; }

# wait_for <label> <timeout_sekunden> <kommando...>
# Ruft <kommando> alle 2 Sekunden auf bis es 0 zurückgibt oder der Timeout läuft.
wait_for() {
    local label="$1" timeout="$2"
    shift 2
    local elapsed=0
    while ! "$@" &>/dev/null; do
        (( elapsed += 2 ))
        if (( elapsed > timeout )); then
            die "Timeout nach ${timeout}s — $label ist nicht bereit. Log prüfen:"$'\n'"  tail -50 $BACKEND_LOG"
        fi
        log "  … warte auf $label (${elapsed}s / ${timeout}s)"
        sleep 2
    done
    log "  ✓ $label bereit"
}

# ─── 1. Stale-Prozesse vom letzten Start beenden ──────────────────────────────

log "Bereinige alte Prozesse …"
pkill -f "quarkus:dev"     2>/dev/null || true
pkill -f "backend-dev.jar" 2>/dev/null || true
pkill -f "vite"            2>/dev/null || true
sleep 1   # kurz warten, damit Ports freigegeben werden

# Letzter Ausweg: falls noch etwas auf 8080 hängt
OLD=$(lsof -ti :8080 2>/dev/null || true)
[[ -n "$OLD" ]] && { log "  Beende Rest-Prozess auf Port 8080 (PID $OLD)"; kill "$OLD" 2>/dev/null || true; sleep 1; }

# ─── 2. Datenbank-Erreichbarkeit prüfen ───────────────────────────────────────

log "Prüfe Datenbank (localhost:5432) …"
wait_for "PostgreSQL" 30 \
    pg_isready -h 127.0.0.1 -p 5432 -U hallen -d hallen

# ─── 3. Backend starten ───────────────────────────────────────────────────────

log "Starte Backend → $BACKEND_LOG"
cd "$ROOT/backend"
./mvnw quarkus:dev -Dquarkus.console.color=false >"$BACKEND_LOG" 2>&1 &
BACKEND_PID=$!
log "  Backend-PID: $BACKEND_PID"

wait_for "Backend HTTP (Port 8080)" 120 \
    curl -sf --max-time 2 http://localhost:8080/halls

# ─── 4. Frontend starten ──────────────────────────────────────────────────────

log "Starte Frontend → $FRONTEND_LOG"
cd "$ROOT/frontend"
npm run dev >"$FRONTEND_LOG" 2>&1 &
FRONTEND_PID=$!
log "  Frontend-PID: $FRONTEND_PID"

wait_for "Frontend (Port 5173)" 30 \
    curl -sf --max-time 2 http://localhost:5173

# ─── 5. Fertig ────────────────────────────────────────────────────────────────

log ""
log "════════════════════════════════════════"
log "  Dev-Stack läuft"
log "  Frontend : http://localhost:5173"
log "  Backend  : http://localhost:8080"
log "  Swagger  : http://localhost:8080/q/swagger-ui/"
log "════════════════════════════════════════"
log ""
log "Logs live:"
log "  tail -f $BACKEND_LOG"
log "  tail -f $FRONTEND_LOG"
log ""
log "Stoppen:  ./stop.sh"
