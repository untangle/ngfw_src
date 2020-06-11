#!/bin/bash

TARBALL=$1
PATCH_NAME=$2

if [ "$TARBALL" = "" ] ; then
    echo "Missing tarball"
    exit
fi
if [ "$PATCH_NAME" = "" ] ; then
    PATCH_NAME=patch.sh
fi

VERSION=$(cat /usr/share/untangle/lib/untangle-libuvm-api/VERSION)

cat << SCRIPT > $PATCH_NAME
if [ `cat /usr/share/untangle/lib/untangle-libuvm-api/VERSION` != '$VERSION' ] ; then 
   echo "This patch can only be run on $VERSION" ;
   exit 1
fi
patch_fs()
{
   /etc/init.d/untangle-vm stop
   openssl enc -d -a | tar -C / -zxf -
   /etc/init.d/untangle-vm start
   echo "Patch Applied."
}
SCRIPT

echo "cat <<ENCODED_PATCH | patch_fs" >> $PATCH_NAME
base64 $TARBALL >> $PATCH_NAME
echo "ENCODED_PATCH" >> $PATCH_NAME

chmod oug+x $PATCH_NAME
