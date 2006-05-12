#!/bin/sh

set -e

# functions -------------------------------------------------------------------

function copyfiles()
{
    while read l; do
        find "$1" -path "$1/$l" -exec cp '{}' "$2" ';'
    done
}

# main ------------------------------------------------------------------------

while getopts :r: KEY
  do
  case ${KEY} in
      r) root="$OPTARG"
  esac
done

libdirs="$root/alpine/output/usr/share/metavize/lib $root/alpine/output/usr/share/java/mvvm"
classpath=$(find $libdirs -name '*.jar' -printf "%p:")

shift $(($OPTIND - 1))

src=$1
dest=$2

classbuild="$dest/WEB-INF/classes"
mkdir -p "$classbuild"

srclist=$(mktemp)
find "$src/src" -name '*.java' >$srclist

$JAVA_HOME/bin/javac -cp "$classpath" -sourcepath "$src/src" -d "$classbuild" \
    @$srclist
rm $srclist

(cd "$src/root" && tar c --exclude '*.svn*' . | tar x -C "$dest")

weblib=$dest/WEB-INF/lib
mkdir $weblib
cat $src/dlinc | copyfiles $root/downloads/output $weblib
