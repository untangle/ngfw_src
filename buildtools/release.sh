#!/bin/sh

version=`head -n 1 debian/changelog | perl -npe 's/.* \((.*)\).*/$1/'`

echo "Releasing alpine packages (version $version)"
echo "------------------------"
dput -c ../pkgs/scripts/dput.cf mephisto ../*${version}*.changes

#hasLocalChanges=`svn status | grep -v -E '^\?'`
#if [[ -z "$hasLocalChanges" ]] ; then
#  svn commit -m"release" debian/changelog
#else
  ##FIXME: I don't think we should check the modified changelog back in
  svn revert debian/changelog
#fi

echo "done."

