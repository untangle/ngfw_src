#!/bin/sh

if [ $# -lt 1 ] ; then
    echo "Usage: $0 package_regex"
    exit 1
fi

if [ "$1" == "all" ] ; then
    echo "Releasing all packages..."
    pkg=""
else
    pkg="$1"
fi

echo "Releasing: \"$1\""


echo "------------------------"
echo -e "Releasing:\n`ls ../*.deb | egrep \"$pkg\"`"
sudo ../pkgs/scripts/deb-add.sh `ls ../*.deb | egrep "$pkg"`
echo "------------------------"
sudo ../pkgs/scripts/deb-scan.sh &> ../update.log
if [ x$email = xfalse ] ; then
    cat debian/release_list | mail -s release pkgs@metavize.com
fi
rm -f debian/release_list
cvs commit -m"release" debian/*.version debian/changelog
echo "done."

