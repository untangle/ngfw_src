#!/bin/bash
##
## Prepare for patching
##
ORIG_PATH=/root/orig
SOURCES_FILE_NAME=/etc/apt/sources.list.d/untangle.list

## Point apt to packages
## deb http://302e-9d2e-5023-5e62:untangle@updates.edge.arista.com/public/bullseye stable-166 main non-free
##
BACKUP_SOURCES_FILE_NAME=/root/untangle.list
if [ ! -f $BACKUP_SOURCES_FILE_NAME ] ; then
	cp -a $SOURCES_FILE_NAME $BACKUP_SOURCES_FILE_NAME
fi

VERSION=$(cat /usr/share/untangle/lib/untangle-libuvm-api/PUBVERSION)

sed -i $SOURCES_FILE_NAME \
    -e 's/updates.edge.arista.com/package-server.untangle.int/' \
    -e "s/stable-[[:digit:]]\+/ngfw-release-$VERSION/"

##
## Backup for comparison.
##
if [ -d $ORIG_PATH ] ; then
	# Generally do not unconditionally update.
	# If we really need patch dependencies, resolve manually
	echo "$ORIG_PATH already exists"
	exit
fi

mkdir -p /root/orig
rsync -av /usr /root/orig
