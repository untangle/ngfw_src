#!/bin/sh

/etc/init.d/postgresql stop

for d in /var/lib/postgresql/* ; do
    if [ -d "${d}" ] ; then
        if [ -d "${d}/main/" ] ; then
            rm -rf ${d}/main/*
        fi
    fi
done

apt-get --yes --reinstall install untangle-postgresql-config

/etc/init.d/postgresql start
/usr/share/untangle/bin/reports-generate-tables.py

echo
echo "done."
