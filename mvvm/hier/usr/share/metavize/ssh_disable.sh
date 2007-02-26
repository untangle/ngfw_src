#!/bin/sh

for PKG_NAME in rbot-mv ssh ; do
  INIT_SCRIPT="/etc/init.d/${PKG_NAME}"

  # Stop the daemon
  ${INIT_SCRIPT} stop

  # Backup the initialization script
  mv ${INIT_SCRIPT} ${INIT_SCRIPT}.tmp

  # Disable startup at boot time
  update-rc.d $PKG_NAME remove > /dev/null

  # Restore the initialization script
  mv ${INIT_SCRIPT}.tmp ${INIT_SCRIPT}
done