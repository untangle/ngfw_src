#!/bin/bash

# update configuration
/usr/sbin/update-exim4.conf

# restart daemon
systemctl restart exim4
RET=$?

# flush queue
/usr/sbin/exim4 -qff

exit $RET