#! /bin/sh

set -e

BACKUP=/var/lib/uvm-settings.sql

if grep -q 5432 /etc/postgresql/7.4/main/postgresql.conf 2> /dev/null || [ -f $BACKUP ] ; then
  echo "Migrating from postgres 7 to postgres 8"

  if dpkg -l postgresql-7.4 | grep -q ii ; then
    echo "  * Backing up existing data"
    pg_dump -n settings -U postgres uvm > $BACKUP
    echo "  * Removing postgresql-7.4"
    apt-get remove --yes postgresql-7.4
  fi

  echo "  * Stopping postgresql-8.3"
  /etc/init.d/postgresql-8.3 stop

  echo "  * Setting postgresql-8.3 to listen on 5432"
  perl -i -pe 's/^port =.*/port = 5432/' /etc/postgresql/8.3/main/postgresql.conf

  echo "  * Starting postgresql-8.3"
  /etc/init.d/postgresql-8.3 start

  echo "  * Creating database and user"
  createuser -U postgres -dSR metavize 2>/dev/null
  createuser -U postgres -dSR untangle 2>/dev/null
  createdb -O postgres -U postgres uvm 2>/dev/null

  echo "  * Restoring data"
  psql -U postgres -f $BACKUP uvm && rm -f $BACKUP

  echo "done"
fi
