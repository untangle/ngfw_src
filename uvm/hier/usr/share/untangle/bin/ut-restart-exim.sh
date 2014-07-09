#!/bin/bash

# update configuration
/usr/sbin/update-exim4.conf

# restart daemon
/etc/init.d/exim4 restart
RET=$?

# flush queue
/usr/sbin/exim4 -qff

exit $RET