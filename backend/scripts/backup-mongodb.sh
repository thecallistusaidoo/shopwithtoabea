#!/usr/bin/env bash
set -Eeuo pipefail
: "${MONGODB_URI:?Set MONGODB_URI before running the backup}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$BACKUP_DIR"
mongodump --uri="$MONGODB_URI" --archive="$BACKUP_DIR/toabea-$STAMP.archive.gz" --gzip --oplog
find "$BACKUP_DIR" -type f -name 'toabea-*.archive.gz' -mtime "+$RETENTION_DAYS" -delete
printf 'Backup created: %s/toabea-%s.archive.gz\n' "$BACKUP_DIR" "$STAMP"
