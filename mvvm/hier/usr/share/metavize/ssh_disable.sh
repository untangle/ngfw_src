#!/bin/sh

# Stop the SSH daemon
/etc/init.d/ssh stop

# Backup the initialization script
mv /etc/init.d/ssh /etc/init.d/ssh.tmp

# Disable ssh startup at bootup
update-rc.d ssh remove > /dev/null

# Restore the ssh initialization script
mv /etc/init.d/ssh.tmp /etc/init.d/ssh
