#!/bin/dash

/etc/init.d/untangle-vm stop


for d in /etc/postgresql/* ; do
    if [ -d "${d}" ] ; then
        if [ -f "${d}/main/postgresql.conf" ] ; then
            sed -i -e "s/.*max_locks_per_transaction.*=.*/max_locks_per_transaction = 2048/" "${d}/main/postgresql.conf"
        fi
    fi
done

if [ -x /etc/init.d/postgresql ] ; then
    /etc/init.d/postgresql stop
    sleep 5
    /etc/init.d/postgresql start
    sleep 10
fi

x=0
while [ $x -le 10 ]
do  
    echo "Drop schema attempt #$x"
    # Drop the reports schema
    dc=$(psql -A -U postgres uvm -c "drop schema reports cascade;" 2>&1)
    echo $dc
    # Parse the stdout to get a substring and see if we see that the schema doesn't exist anymore
    case "$dc" in
    *'schema "reports" does not exist'*) break ;;
    esac

    x=$((x+1))
done

for d in /etc/postgresql/* ; do
    if [ -d "${d}" ] ; then
        if [ -f "${d}/main/postgresql.conf" ] ; then
            sed -i -e "s/.*max_locks_per_transaction.*=.*/#max_locks_per_transaction = 64/" "${d}/main/postgresql.conf"
        fi
    fi
done

if [ -x /etc/init.d/postgresql ] ; then
    /etc/init.d/postgresql reload
fi

if [ $? -eq 0 ] ; then
    /bin/echo -e '\n\nReports schema successfully cleared.'
else
    /bin/echo -e '\n\nAn error occurred. Please contact support.'
fi

/etc/init.d/untangle-vm start


