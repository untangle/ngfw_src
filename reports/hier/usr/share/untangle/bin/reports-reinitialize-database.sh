#!/bin/sh

set -eu

# Reports must operate on the registered cluster serving the appliance's
# PostgreSQL endpoint.  The default psql client may belong to another major
# version, and deleting every /var/lib/postgresql/* cluster is destructive.
CLUSTER_INFO=$(pg_lsclusters -h | awk '$2 == "main" && $3 == "5432" { print; exit }')
if [ -z "$CLUSTER_INFO" ]; then
    echo "No registered PostgreSQL main cluster is serving port 5432" >&2
    exit 1
fi

PG_VERSION=$(printf '%s\n' "$CLUSTER_INFO" | awk '{print $1}')
PG_STATUS=$(printf '%s\n' "$CLUSTER_INFO" | awk '{print $4}')
case "$PG_VERSION" in
    ''|*[!0-9]*)
        echo "Invalid PostgreSQL cluster version: $PG_VERSION" >&2
        exit 1
        ;;
esac

PG_VAR_DIR="/var/lib/postgresql/${PG_VERSION}"
PG_MAIN_DIR="${PG_VAR_DIR}/main"
PG_BIN_DIR="/usr/lib/postgresql/${PG_VERSION}/bin"

if [ ! -d "$PG_MAIN_DIR" ] || [ ! -x "$PG_BIN_DIR/initdb" ]; then
    echo "Registered PostgreSQL cluster paths are missing" >&2
    exit 1
fi

if [ "$PG_STATUS" = "online" ]; then
    pg_ctlcluster "$PG_VERSION" main stop
fi

# Reinitialize only the selected cluster.  The path guard prevents an empty or
# malformed version value from expanding into a broad deletion.
case "$PG_MAIN_DIR" in
    /var/lib/postgresql/[0-9]*/main) ;;
    *) echo "Refusing to reinitialize unexpected path: $PG_MAIN_DIR" >&2; exit 1 ;;
esac
find "$PG_MAIN_DIR" -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +

locale=${1:-en_US.UTF-8}
su -s /bin/sh postgres -c \
    "\"$PG_BIN_DIR/initdb\" --encoding=utf8 --locale=\"$locale\" -D \"$PG_MAIN_DIR\"" \
    >/dev/null

echo "done."
