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
echo -e "Releasing:\n`ls ../*.changes | egrep \"$pkg\"`"
dput -c ../pkgs/scripts/dput.cf mephisto ../`head -n 1 debian/changelog | perl -npe 's/ \((.*)\).*/*$$1/'`*.changes

rm -f debian/release_list
svn commit -m"release" debian/*.version debian/changelog
echo "done."

