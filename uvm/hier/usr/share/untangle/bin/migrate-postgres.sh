#! /bin/sh

set -e

#
# Possible cases:
# Postgres 8 is running on port 5432.  Who cares, there is nothing to do.
## Possibly copy the BACKUP file to /usr/share/untangle/autodump, for safety.
# Postgres 7 is running.
## * A. There is a backup file.  It is probably out of date, use the new one.
## * B. There is no backup file.  Use the new one.
# Postgres 7 is not running.
## * A. There is a backup file : Do the migration.
## * B. There is no backup file.  Babysitters / UVM startup script will take it from here.

BACKUP=/var/lib/uvm-settings.sql

is_postgres_8_enabled()
{
    ## If it is configured for 5432, then we assume the babysitter will take care of it.
    echo "[`date`] Searching for port 5433 in the postgres config"

    ## If it is configured for 5433, it is not enabled.
    if grep -E "port[ ]*=[ ]*5433" /etc/postgresql/8.3/main/postgresql.conf ; then
        echo "[`date`] postgres is set to port 5433"
        return 1
    fi

    echo "[`date`] postgres 8 is not set to port 5433"

    return 0
}

is_postgres_7_running()
{
    ## This assumes that postgres 8 is not running.
    if [ ! -f /etc/postgresql/7.4/main/postgresql.conf ]; then
        return 1
    fi

    if [ ! -e /var/run/postgresql/.s.PGSQL.5432 ]; then
        return 1
    fi
    
    return 0    
}

enable_postgres_8()
{
    echo "[`date`]  * Stopping postgresql-8.3"
    /etc/init.d/postgresql-8.3 stop
    
    echo "[`date`]  * Setting postgresql-8.3 to listen on 5432"
    perl -i -pe 's/^port =.*/port = 5432/' /etc/postgresql/8.3/main/postgresql.conf
    
    echo "[`date`]  * Starting postgresql-8.3"
    /etc/init.d/postgresql-8.3 start
}

remove_postgres_7()
{
    echo "[`date`]  * Removing postgres 7"
    ## Try to stop it if a file from the package still exists.
    if [ -x /etc/init.d/postgresql-7.4 -a -f /usr/lib/postgresql/7.4/bin/postgres ]; then
        echo "[`date`]  * Stopping postgres 7"
        /etc/init.d/postgresql-7.4 stop || true
    fi

    ## First try to remove this config file (it can't start without it).
    echo "[`date`]  * Removing postgres 7 config file"
    rm -f /etc/postgresql/7.4/main/postgresql.conf
    
    ## Try to remove the package.
    if [ -f /usr/lib/postgresql/7.4/bin/postgres ]; then
        echo "[`date`]  * Removing postgres 7 package"
        apt-get remove --yes postgresql-7.4 || true
    fi
}

migrate_data()
{
    ## First make sure that postgres 8 is running.
    if [ ! -e /var/run/postgresql/.s.PGSQL.5433 ]; then
        echo "[`date`] Starting postgresql-8.3"
        /etc/init.d/postgresql-8.3 start
        if [ ! -e /var/run/postgresql/.s.PGSQL.5433 ]; then
            echo "[`date`]  Unable to start postgres-8.3, exiting"
            exit 1
        fi
    fi
    
    echo "[`date`] Creating database and user"
    /usr/lib/postgresql/8.3/bin/createuser -p 5433 -U postgres -dSR metavize || true
    /usr/lib/postgresql/8.3/bin/createuser -p 5433 -U postgres -dSR untangle || true

    # Drop that UVM database, and exit if it is unable to remove it
    psql -p 5433 -U postgres -c "DROP DATABASE IF EXISTS uvm" || {
        echo "[`date`] Unable to drop database in postgres 8, giving up"
        exit 1
    }

    createdb -p 5433 -O postgres -U postgres uvm 2>/dev/null
    
    echo "[`date`] Restoring data"
    psql -p 5433 -U postgres -f $BACKUP uvm
    
    echo "[`date`] Clearing events_version"
    psql -p 5433 -U postgres -c "UPDATE settings.split_schema_ver SET events_version = NULL;" uvm

    rm -f $BACKUP
}


### Start of the script
## it is already running on postgres 8, just try to remove postgres 7.
if is_postgres_8_enabled; then
    echo "[`date`]  * Postgres 8 is enabled, trying to remove postgres 7."
    remove_postgres_7
    exit 0
fi

if is_postgres_7_running ; then
    ## Generate a new backup file.
    echo "[`date`] Backing up existing data."
    pg_dump -n settings -U postgres uvm > $BACKUP
    echo "[`date`] Backup completed."
fi

## If a backup file exists, try to load it into postgres 8.
if [ -f ${BACKUP} ]; then
    migrate_data
fi

# disable error checking, everything past here should just run.
set +e

echo "[`date`]  * Migration is complete, disabling postgres 7 and enabling postgres 8."

## remove postgres 7.
remove_postgres_7

## enable postgres 8.
enable_postgres_8

# fin.
