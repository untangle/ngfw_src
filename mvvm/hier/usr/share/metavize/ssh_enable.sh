#!/bin/sh

## Start the ssh daemon
/etc/init.d/ssh start

## Update to start SSH at bootup
update-rc.d ssh defaults > /dev/null



