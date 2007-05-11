#!/bin/sh

[[ ! $# -eq 1 ]] && echo "Usage: $0 distribution" && exit 1

distribution=${1}

echo -n "Setting version, setting distribution to \"$distribution\"..."

if [[ -f ../VERSION ]] ; then
  versionFile=../VERSION
else
  versionFile=../../VERSION
fi

revision=`svn info . | awk '/Last Changed Rev: / { print $4 }'`
timestamp=`svn info | awk '/Last Changed Date: / { gsub(/-/, "", $4) ; print $4 }'`
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

dch -v ${upstreamVersion}-1 -D ${distribution} "auto build"

echo " done."

