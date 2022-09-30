#!/bin/bash
# arch default to the build arch
BUILD_ARCHITECTURE=$(dpkg-architecture -qDEB_BUILD_ARCH)
ARCHITECTURE=${ARCHITECTURE:-${BUILD_ARCHITECTURE}}

# set base distribution if none was passed
if [[ -z "$DISTRIBUTION" ]] ; then
  DISTRIBUTION=$(cat $PKGTOOLS/resources/DISTRIBUTION)
fi

echo "deb http://package-server/public/$REPOSITORY $DISTRIBUTION main non-free" > /etc/apt/sources.list.d/${DISTRIBUTION}.list
apt-get update
apt -o Dpkg::Options::="--force-overwrite" build-dep -y --host-architecture $ARCHITECTURE .

if [ "$BUILD_TYPE" = "rake" ] ; then
  # Full uvm build
  echo "RAKE_LOG=$RAKE_LOG"
  rm -f $RAKE_LOG
  rake |& tee $RAKE_LOG
elif [ "$BUILD_TYPE" = "i18n" ] ; then
  # i18n template build
  cd i18ntools
  ./generate.py
  cd -
fi
