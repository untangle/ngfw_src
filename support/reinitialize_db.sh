#!/bin/sh

/etc/init.d/postgresql stop

if [ -d /var/lib/postgresql/9.4/main ] ; then
    rm -rf /var/lib/postgresql/9.4/main/*
fi
if [ -d /var/lib/postgresql/9.6/main ] ; then
    rm -rf /var/lib/postgresql/9.6/main/*
fi
apt-get --yes --reinstall install untangle-postgresql-config

/etc/init.d/postgresql start
/usr/share/untangle/bin/reports-generate-tables.py

echo
echo "done."
