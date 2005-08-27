#!/bin/sh

if [ $# -lt 1 ] ; then
    echo "Usage: $0 package_regex destination"
    exit 1
fi

if [ "$1" == "all" ] ; then
    echo "Releasing all packages..."
    pkg=""
else
    pkg="$1"
fi

if [ -z "$2" ] ; then
    dest="`whoami`"
else
    dest="$2"
fi

echo "Releasing: \"$1\""


echo "------------------------"
echo -e "Releasing:\n`ls ../*.deb | egrep \"$pkg\"`"
sudo ../pkgs/scripts/deb-add.sh /var/www/$dest/ `ls ../*.deb | egrep "$pkg"`
echo "------------------------"
sudo ../pkgs/scripts/deb-scan.sh /var/www/$dest/ 

rm -f debian/release_list
svn commit -m"release" debian/*.version debian/changelog
echo "done."

