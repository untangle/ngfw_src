#!/bin/sh

[[ ! $# -eq 1 ]] && echo "Usage: $0 distribution" && exit 1

distribution=${1}

echo -n "Setting version, setting distribution to \"$distribution\"..."

if [[ -f ../VERSION ]] ; then
  versionFile=../VERSION
else
  versionFile=../../VERSION
fi

revision=`svn info . | awk '/Revision: / { print $2 }'`
timestamp=`svn info | awk '/Last Changed Date: / { gsub(/-/, "", $4) ; print $4 }'`
hasLocalChanges=`svn status | grep -v -E '^\?'`

baseVersion=`cat $versionFile`~svn${timestamp}r${revision}

if [[ -z "$hasLocalChanges" ]] ; then
  upstreamVersion=$baseVersion
else
  upstreamVersion=${baseVersion}+$USER`date +"%Y%m%dT%H%M"`
  distribution=$USER
fi

previousUpstreamVersion=`dpkg-parsechangelog | awk '/Version: / { gsub(/-.*/, "", $2) ; print $2 }'`

if [[ "${upstreamVersion}" == "$previousUpstreamVersion" ]] ; then
  dchArg="-i"
else
  dchArg="-v ${upstreamVersion}-1"
fi

dch $dchArg -D ${distribution} "auto build"

echo " done."

