#!/bin/sh

/etc/init.d/postgresql stop

for d in /var/lib/postgresql/* ; do
    if [ -d "${d}" ] ; then
        if [ -d "${d}/main/" ] ; then
            rm -rf ${d}/main/*
        fi
    fi
done

# Recreating the createDB() logic from untangle-postgresql-config.postinst
if grep -qE '^11\.' /etc/debian_version ; then
  PG_VERSION="13"
else
  PG_VERSION="11"
fi
PG_VAR_DIR="/var/lib/postgresql/${PG_VERSION}"
PG_BIN_DIR="/usr/lib/postgresql/${PG_VERSION}/bin"
su -c "${PG_BIN_DIR}/initdb --encoding=utf8 --locale=${1:-"en_US.UTF-8"} -D ${PG_VAR_DIR}/main" postgres

/etc/init.d/postgresql start
/usr/share/untangle/bin/reports-generate-tables.py

echo
echo "done."
