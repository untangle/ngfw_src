#!/bin/sh

# usage...
[ ! $# -eq 1 ] && echo "Usage: $0 distribution" && exit 1

# CL args
distribution=${1}
case $distribution in
  # FIXME: somehow only codenames work for uploading, right now
  testing) distribution=mustang ;;
esac

if [ -f ../VERSION ] ; then
  versionFile=../VERSION
elif [ -f ./resources/VERSION ] ; then # Hades
  versionFile=./resources/VERSION
else
  versionFile=../../VERSION
fi

# get 2 values from SVN: last changed revision & timestamp for the
# current directory
revision=`svn info --recursive . | awk '/Last Changed Rev: / { print $4 }' | sort -n | tail -1`
timestamp=`svn info --recursive . | awk '/Last Changed Date:/ { gsub(/-/, "", $4) ; print $4 }' | sort -n | tail -1`

# this is how we figure out if we're up-to-date or not
hasLocalChanges=`svn status | grep -v -E '^([X?]|Fetching external item into|Performing status on external item at|$)'`

# this is the base version; it will be tweaked a bit oif need be:
# - append a local modification marker is we're not up to date
# - prepend the upstream version if UNTANGLE-KEEP-UPSTREAM-VERSION exists
baseVersion=`cat $versionFile`~svn${timestamp}r${revision}

if [ -f UNTANGLE-KEEP-UPSTREAM-VERSION ] ; then
  previousUpstreamVersion=`dpkg-parsechangelog | awk '/Version: / { gsub(/-.*/, "", $2) ; print $2 }'`
  baseVersion=${previousUpstreamVersion}+${baseVersion}
fi

if [ -z "$hasLocalChanges" ] ; then
  version=$baseVersion
else
  version=${baseVersion}+$USER`date +"%Y%m%dT%H%M%S"`
  distribution=$USER
fi

version=${version}-1

echo "Setting version to \"${version}\", distribution to \"$distribution\""
DEBEMAIL="${DEBEMAIL:-${USER}@untangle.com}" dch -v ${version} -D ${distribution} "auto build"
echo " done."


