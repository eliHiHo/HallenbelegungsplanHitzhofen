#!/usr/bin/env bash
# PostgreSQL backup: dump → local storage → remote VPS via rsync/SSH
# Reads config from .env in the same directory.
# Run via cron; exits non-zero on any failure so cron can report it.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"

# Refuse to run if .env is readable by anyone other than owner
ENV_PERMS=$(stat -c %a "${ENV_FILE}")
[ "${ENV_PERMS}" = "600" ] || {
    echo "ERROR: ${ENV_FILE} must be chmod 600 (current: ${ENV_PERMS}). Aborting."
    exit 1
}

# Load .env (provides DB_PASSWORD and BACKUP_* variables)
# shellcheck disable=SC1091
source "${ENV_FILE}"

# ── Config (all overridable via .env) ────────────────────────────────────────
BACKUP_DIR="${BACKUP_DIR:-/opt/hallenbelegungsplan/backups}"
LOCAL_KEEP_DAYS="${BACKUP_KEEP_DAYS_LOCAL:-14}"
REMOTE_KEEP_DAYS="${BACKUP_KEEP_DAYS_REMOTE:-60}"
DB_CONTAINER="${BACKUP_DB_CONTAINER:-hallen-db}"
DB_NAME="${BACKUP_DB_NAME:-hallen}"
DB_USER="${BACKUP_DB_USER:-hallen}"
# BACKUP_REMOTE_HOST, BACKUP_REMOTE_USER, BACKUP_REMOTE_DIR, BACKUP_SSH_KEY
# are optional — remote transfer is skipped if BACKUP_REMOTE_HOST is unset.

log() { echo "[$(date -Iseconds)] backup: $*"; }

# ── 1. Dump ───────────────────────────────────────────────────────────────────
mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DUMP_FILE="${BACKUP_DIR}/hallen_${TIMESTAMP}.sql.gz"
DUMP_TMP="${DUMP_FILE}.tmp"

# Remove temp file on any exit; write a failure marker to BACKUP_DIR on error
trap '[ -n "${DUMP_TMP:-}" ] && rm -f "${DUMP_TMP}"' EXIT
trap 'echo "[$(date -Iseconds)] FAILED" > "${BACKUP_DIR}/last_backup_status"' ERR

log "dumping ${DB_NAME} from container ${DB_CONTAINER}..."
docker exec \
    -e PGPASSWORD="${DB_PASSWORD}" \
    "${DB_CONTAINER}" \
    pg_dump -U "${DB_USER}" -d "${DB_NAME}" \
    | gzip > "${DUMP_TMP}"

mv "${DUMP_TMP}" "${DUMP_FILE}"

gzip -t "${DUMP_FILE}" || { log "ERROR: dump failed gzip integrity check"; exit 1; }
DUMP_BYTES=$(stat -c%s "${DUMP_FILE}")
[ "${DUMP_BYTES}" -ge 4096 ] || { log "ERROR: dump suspiciously small (${DUMP_BYTES} bytes)"; exit 1; }

log "saved: ${DUMP_FILE} ($(du -sh "${DUMP_FILE}" | cut -f1))"

# ── 2. Remote transfer (skipped if BACKUP_REMOTE_HOST is not set) ────────────
if [ -n "${BACKUP_REMOTE_HOST:-}" ]; then
    SSH_KEY="${BACKUP_SSH_KEY:-${HOME}/.ssh/id_ed25519}"
    REMOTE_USER="${BACKUP_REMOTE_USER:-backup}"
    REMOTE_DIR="${BACKUP_REMOTE_DIR:-/backups/hallenbelegungsplan}"
    SSH_OPTS="-i ${SSH_KEY} -o BatchMode=yes -o StrictHostKeyChecking=yes"

    log "transferring to ${REMOTE_USER}@${BACKUP_REMOTE_HOST}:${REMOTE_DIR}/ ..."
    rsync -az \
        -e "ssh ${SSH_OPTS}" \
        "${DUMP_FILE}" \
        "${REMOTE_USER}@${BACKUP_REMOTE_HOST}:${REMOTE_DIR}/"

    # Remote retention cleanup
    # shellcheck disable=SC2029
    ssh ${SSH_OPTS} \
        "${REMOTE_USER}@${BACKUP_REMOTE_HOST}" \
        "find ${REMOTE_DIR} -name 'hallen_*.sql.gz' -mtime +${REMOTE_KEEP_DAYS} -delete"
    log "remote transfer done; remote retention: ${REMOTE_KEEP_DAYS} days."
fi

# ── 3. Local retention ────────────────────────────────────────────────────────
find "${BACKUP_DIR}" -name "hallen_*.sql.gz" -mtime "+${LOCAL_KEEP_DAYS}" -delete
log "local retention: kept last ${LOCAL_KEEP_DAYS} days."

echo "[$(date -Iseconds)] OK ${DUMP_FILE}" > "${BACKUP_DIR}/last_backup_status"
log "done."
