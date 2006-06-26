#!/bin/sh

MVVM_FILENAME="`ls -t ../| grep -e mvvm_.*deb | head -n 1`" 
NAT_FILENAME="`ls -t ../| grep -e nat-transform_.*deb | head -n 1`" 
echo $NAT_FILENAME $MVVM_FILENAME 

rm -rf /tmp/mvvm-rebuild
mkdir -p /tmp/mvvm-rebuild
mv ../$MVVM_FILENAME /tmp/mvvm-rebuild/

pushd . &> /dev/null

cd /tmp/mvvm-rebuild/
ar x $MVVM_FILENAME
tar --same-owner -x -z -f data.tar.gz
rm -f $MVVM_FILENAME
rm -f ./debian-binary 
rm -f ./.deb
mkdir -p DEBIAN
mv control.tar.gz DEBIAN/
cd DEBIAN/
tar -x -z --same-owner -f control.tar.gz
rm -f control.tar.gz
cd ../
sudo chown -R root:root ./DEBIAN
rm -f data.tar.gz
sudo chown -R root:root ./usr/share/doc/mvvm

popd &> /dev/null

cp -f ../$NAT_FILENAME /tmp/mvvm-rebuild/var/lib/mvvm/nat-transform.deb
dpkg-deb -b /tmp/mvvm-rebuild/ ../$MVVM_FILENAME
rm -rf /tmp/mvvm-rebuild