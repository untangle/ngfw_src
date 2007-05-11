#!/bin/sh

# usage...
[[ ! $# -eq 1 ]] && echo "Usage: $0 distribution" && exit 1

# CL args
distribution=${1}
case $distribution in
  # FIXME: somehow only codenames work for uploading, right now
  testing) distribution=mustang ;;
esac

echo -n "Setting version, setting distribution to \"$distribution\""

if [[ -f ../VERSION ]] ; then
  versionFile=../VERSION
else
  versionFile=../../VERSION
fi

# get 2 values from SVN: last changed revision & timestamp for the
# current directory
revision=`svn info . | awk '/Last Changed Rev: / { print $4 }'`
timestamp=`svn info . | awk '/Last Changed Date: / { gsub(/-/, "", $4) ; print $4 }'`

# this how we figure out if we're up-to-date or not
hasLocalChanges=`svn status | grep -v -E '^\?'`

baseVersion=`cat $versionFile`~svn${timestamp}r${revision}

previousUpstreamVersion=`dpkg-parsechangelog | awk '/Version: / { gsub(/-.*/, "", $2) ; print $2 }'`

if [[ -f KEEP-UPSTREAM-VERSION ]] ; then
  baseVersion=${previousUpstreamVersion}+${baseVersion}
fi

if [[ -z "$hasLocalChanges" ]] ; then
  upstreamVersion=$baseVersion
else
  upstreamVersion=${baseVersion}+$USER`date +"%Y%m%dT%H%M%S"`
  distribution=$USER
fi

echo -n ", \"version\" to ${upstreamVersion}-1"
dch -v ${upstreamVersion}-1 -D ${distribution} "auto build"

echo " done."

