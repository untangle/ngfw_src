#!/bin/sh

#/etc/init.d/postgresql stop >/dev/null 2>&1

for d in /var/lib/postgresql/* ; do
    if [ -d "${d}" ] ; then
        if [ -d "${d}/main/" ] ; then
            rm -rf ${d}/main/*
        fi
    fi
done

# Recreating the createDB() logic from untangle-postgresql-config.postinst
PG_VERSION=$(psql --version | sed 's/^[^0-9]*\([0-9]\+\).*/\1/')
PG_VAR_DIR="/var/lib/postgresql/${PG_VERSION}"
PG_BIN_DIR="/usr/lib/postgresql/${PG_VERSION}/bin"
su -c "${PG_BIN_DIR}/initdb --encoding=utf8 --locale=${1:-"en_US.UTF-8"} -D ${PG_VAR_DIR}/main" postgres >/dev/null 2>&1

#/etc/init.d/postgresql start >/dev/null 2>&1
#/usr/share/untangle/bin/reports-generate-tables.py >/dev/null 2>&1

echo "done."
