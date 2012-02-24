#!/bin/sh

# update configuration
/usr/sbin/update-exim4.conf

# restart daemon
/etc/init.d/exim4 restart

# flush queue
/usr/sbin/exim4 -qff