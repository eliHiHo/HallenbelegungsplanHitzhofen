#!/usr/bin/env bash
# Restore a PostgreSQL dump into the hallen database.
# Usage: ./restore.sh <path-to-dump.sql.gz>
#
# Steps:
#   1. Verify the dump file is valid
#   2. Stop the backend container
#   3. Terminate active DB connections
#   4. Drop and recreate the target database
#   5. Restore the dump
#   6. Restart the backend container
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"

log() { echo "[$(date -Iseconds)] restore: $*"; }
die() { log "ERROR: $*" >&2; exit 1; }

# ── Usage ─────────────────────────────────────────────────────────────────────
[ $# -eq 1 ] || die "Usage: $0 <path-to-dump.sql.gz>"
DUMP_FILE="$1"

# ── Security: refuse to run if .env is readable by others ────────────────────
ENV_PERMS=$(stat -c %a "${ENV_FILE}")
[ "${ENV_PERMS}" = "600" ] || die "${ENV_FILE} must be chmod 600 (current: ${ENV_PERMS})"

# ── Load config ───────────────────────────────────────────────────────────────
# shellcheck disable=SC1091
source "${ENV_FILE}"

DB_CONTAINER="${BACKUP_DB_CONTAINER:-hallen-db}"
DB_NAME="${BACKUP_DB_NAME:-hallen}"
DB_USER="${BACKUP_DB_USER:-hallen}"
BACKEND_CONTAINER="hallen-backend"

# ── Pre-flight checks ─────────────────────────────────────────────────────────
[ -f "${DUMP_FILE}" ] || die "Dump file not found: ${DUMP_FILE}"

log "verifying dump integrity..."
gzip -t "${DUMP_FILE}" || die "Dump file failed gzip integrity check: ${DUMP_FILE}"

DUMP_BYTES=$(stat -c%s "${DUMP_FILE}")
[ "${DUMP_BYTES}" -ge 4096 ] || die "Dump file suspiciously small (${DUMP_BYTES} bytes): ${DUMP_FILE}"

docker inspect "${DB_CONTAINER}" > /dev/null 2>&1 \
    || die "Database container not found: ${DB_CONTAINER}"
docker inspect -f '{{.State.Running}}' "${DB_CONTAINER}" | grep -q "true" \
    || die "Database container is not running: ${DB_CONTAINER}"

# ── Confirmation ──────────────────────────────────────────────────────────────
echo ""
echo "  Target database   : ${DB_NAME}"
echo "  DB container      : ${DB_CONTAINER}"
echo "  Backend container : ${BACKEND_CONTAINER}"
echo "  Dump file         : ${DUMP_FILE} ($(du -sh "${DUMP_FILE}" | cut -f1))"
echo ""
echo "  WARNING: This will permanently DROP and RECREATE the '${DB_NAME}' database."
echo "           All existing data will be lost."
echo ""
printf "  Type 'yes' to continue: "
read -r CONFIRM
[ "${CONFIRM}" = "yes" ] || die "Aborted by user."
echo ""

# ── Stop backend ──────────────────────────────────────────────────────────────
log "stopping ${BACKEND_CONTAINER}..."
if docker inspect -f '{{.State.Running}}' "${BACKEND_CONTAINER}" 2>/dev/null | grep -q "true"; then
    docker stop "${BACKEND_CONTAINER}"
    log "${BACKEND_CONTAINER} stopped."
else
    log "${BACKEND_CONTAINER} was not running, continuing."
fi

# ── Restore ───────────────────────────────────────────────────────────────────
log "terminating active connections to '${DB_NAME}'..."
docker exec \
    -e PGPASSWORD="${DB_PASSWORD}" \
    "${DB_CONTAINER}" \
    psql -U "${DB_USER}" -d postgres -q \
    -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '${DB_NAME}' AND pid <> pg_backend_pid();" \
    > /dev/null

log "dropping database '${DB_NAME}'..."
docker exec \
    -e PGPASSWORD="${DB_PASSWORD}" \
    "${DB_CONTAINER}" \
    psql -U "${DB_USER}" -d postgres -q \
    -c "DROP DATABASE IF EXISTS ${DB_NAME};"

log "recreating database '${DB_NAME}'..."
docker exec \
    -e PGPASSWORD="${DB_PASSWORD}" \
    "${DB_CONTAINER}" \
    psql -U "${DB_USER}" -d postgres -q \
    -c "CREATE DATABASE ${DB_NAME} OWNER ${DB_USER};"

log "restoring dump (this may take a moment)..."
gunzip -c "${DUMP_FILE}" \
    | docker exec -i \
        -e PGPASSWORD="${DB_PASSWORD}" \
        "${DB_CONTAINER}" \
        psql -U "${DB_USER}" -d "${DB_NAME}" -q \
        -v ON_ERROR_STOP=1

log "restore complete."

# ── Restart backend ───────────────────────────────────────────────────────────
log "starting ${BACKEND_CONTAINER}..."
docker start "${BACKEND_CONTAINER}"
log "${BACKEND_CONTAINER} started. Monitor with: docker logs -f ${BACKEND_CONTAINER}"
