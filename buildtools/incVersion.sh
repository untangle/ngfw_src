#!/bin/sh

[[ ! $# -eq 1 ]] && echo "Usage: $0 distribution" && exit 1

distribution=${1}

echo -n "Setting version, setting distribution to \"$distribution\"..."

if [[ ! -d ../pkgs ]] ; then
  echo "You must check out the pkgs subversion directory" && exit 1
fi

revision=`svn info . | awk '/Revision: / { print $2 }'`
timestamp=`svn info | awk '/Last Changed Date: / { gsub(/-/, "", $4) ; print $4 }'`
hasLocalChanges=`svn status | grep -v -E '^\?'`

baseVersion=`cat VERSION`~svn${timestamp}r${revision}

if [[ -z "$hasLocalChanges" ]] ; then
  upstreamVersion=$baseVersion
else
  upstreamVersion=${baseVersion}+$USER`date +"%Y%m%dT%H%M"`
fi

previousUpstreamVersion=`dpkg-parsechangelog | awk '/Version: / { gsub(/-.*/, "", $2) ; print $2 }'`

if [[ "${upstreamVersion}" == "$previousUpstreamVersion" ]] ; then
  dchArg="-i"
else
  dchArg="-v ${upstreamVersion}-1"
fi

dch $dchArg -D ${distribution} "auto build"

echo " done."

