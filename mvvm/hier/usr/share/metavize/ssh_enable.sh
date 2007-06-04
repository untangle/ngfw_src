#!/bin/sh

apt-get install --yes untangle-support-agent

for PKG_NAME in untangle-support-agent ssh ; do
  INIT_SCRIPT="/etc/init.d/${PKG_NAME}"

  # Start the daemon
  ${INIT_SCRIPT} start

  # Enable startup at boot time
  update-rc.d ${PKG_NAME} defaults > /dev/null
done


