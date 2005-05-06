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

echo "Incrementing Versions: \"$1\""


if [ ! -d ../pkgs ] ; then
    echo "You must check out pkgs cvs module" ; exit 1
elif [ ! "`hostname`" = "bebe" ] ; then
    echo "Must be run on bebe"; exit 1
fi

echo "Incrementing source package version."
dch -i "auto build"

echo "Incrementing \"$pkg*deb\" versions:"
echo > ./debian/release_list

packages="`ls ./debian/*version | egrep \"$pkg\" `"
for i in $packages ; do
    cat $i | perl -pe 's/(\d+)(\D*)$/($1+1).$pkg/e' > $i.tmp
    echo " -> " $i " (version: " "`cat $i.tmp`"  " )"
    rm -f $i
    mv $i.tmp $i
    echo "`echo $i | sed -e 's/.version//' | sed -e 'sl./debian/ll'`" "`cat ../VERSION && cat $i`" " released. " >> ./debian/release_list
done
echo > ./debian/release_list

echo "Done."

