#!/bin/dash

/etc/init.d/untangle-vm stop


for ver in 9.1 9.4 9.6 ; do
  if [ -f /etc/postgresql/$ver/main/postgresql.conf ] ; then
    sed -i -e "s/.*max_locks_per_transaction.*=.*/max_locks_per_transaction = 2048/" /etc/postgresql/$ver/main/postgresql.conf
  fi
done

if [ -x /etc/init.d/postgresql ] ; then
    /etc/init.d/postgresql stop
    sleep 5
    /etc/init.d/postgresql start
    sleep 10
fi

psql -U postgres uvm -c"drop schema reports cascade;"

for ver in 9.1 9.4 9.6 ; do
  if [ -f /etc/postgresql/$ver/main/postgresql.conf ] ; then
    sed -i -e "s/.*max_locks_per_transaction.*=.*/#max_locks_per_transaction = 64/" /etc/postgresql/$ver/main/postgresql.conf
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


