#!/usr/bin/env bash
# Drops and recreates the CI database between test steps.
#
# Several integration tests truncate their own tables, so each step starts from
# an empty schema rather than whatever the previous step left behind.
set -euo pipefail

DB_NAME="${CI_DATABASE_NAME:-CS3227}"
DB_HOST="${CI_DATABASE_HOST:-localhost}"
DB_USER="${CI_DATABASE_USER:-postgres}"
export PGPASSWORD="${CI_DATABASE_PASSWORD:-postgres}"

psql -h "$DB_HOST" -U "$DB_USER" -d postgres -v ON_ERROR_STOP=1 \
  -c "DROP DATABASE IF EXISTS \"$DB_NAME\";"
psql -h "$DB_HOST" -U "$DB_USER" -d postgres -v ON_ERROR_STOP=1 \
  -c "CREATE DATABASE \"$DB_NAME\";"
