#!/bin/bash

VERSION=$1
TARBALL=$2
PATCH_NAME=$3

if [ "$VERSION" = "" ] ; then
    echo "Version is required, in full format like 16.6.0"
    exit
elif [ "$VERSION" = $(cat /usr/share/untangle/lib/untangle-libuvm-api/VERSION) ] ; then
    echo "WARNING: $VERSION matches current system; it may need to be one less?"
fi

if [ "$TARBALL" = "" ] ; then
    TARBALL=patch.tgz
fi
if [ "$PATCH_NAME" = "" ] ; then
    PATCH_NAME=patch.sh
fi

cat << SCRIPT > $PATCH_NAME
if [ \`cat /usr/share/untangle/lib/untangle-libuvm-api/VERSION\` != '$VERSION' ] ; then 
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

echo "Created $PATCH_NAME for VERSION=$VERSION"
