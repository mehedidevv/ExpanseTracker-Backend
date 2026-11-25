#!/usr/bin/env bash
# Creates the local PostgreSQL role + databases used by the app and the tests.
# Safe to run multiple times. Requires a running PostgreSQL 15+ and a superuser connection.
#
# Usage: ./scripts/setup-local-db.sh
# Override with env vars: PGHOST, PGPORT, PGSUPERUSER, DB_NAME, DB_USERNAME, DB_PASSWORD
set -euo pipefail

PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGSUPERUSER="${PGSUPERUSER:-$(whoami)}"
DB_NAME="${DB_NAME:-expense_tracker}"
DB_USERNAME="${DB_USERNAME:-expense}"
DB_PASSWORD="${DB_PASSWORD:-expense}"
TEST_DB_NAME="${DB_NAME}_test"

psql_admin() { psql -h "$PGHOST" -p "$PGPORT" -U "$PGSUPERUSER" -d postgres -v ON_ERROR_STOP=1 -qtA "$@"; }

echo "→ PostgreSQL: $(psql_admin -c 'show server_version')"

if [[ "$(psql_admin -c "select 1 from pg_roles where rolname='${DB_USERNAME}'")" != "1" ]]; then
  psql_admin -c "create role ${DB_USERNAME} login password '${DB_PASSWORD}'"
  echo "✓ role ${DB_USERNAME} created"
else
  psql_admin -c "alter role ${DB_USERNAME} login password '${DB_PASSWORD}'"
  echo "✓ role ${DB_USERNAME} exists (password reset)"
fi

for db in "$DB_NAME" "$TEST_DB_NAME"; do
  if [[ "$(psql_admin -c "select 1 from pg_database where datname='${db}'")" != "1" ]]; then
    psql_admin -c "create database ${db} owner ${DB_USERNAME} encoding 'UTF8'"
    echo "✓ database ${db} created"
  else
    echo "✓ database ${db} exists"
  fi
done

echo
echo "Done. Tables are created by Flyway on first app start: ./mvnw spring-boot:run"
