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

while getopts :r:n: KEY
  do
  case ${KEY} in
      r) root="$OPTARG";;
      n) name="$OPTARG";;
  esac
done

root=$1
name=$2
src=$3
dest=$4

uriroot="$src/root"
classroot="$dest/WEB-INF/classes"

libdirs="$root/alpine/output/usr/share/metavize/lib $root/alpine/output/usr/share/java/mvvm $root/downloads/output/jakarta-tomcat-5.0.28-embed/lib"

classpath="$(find $libdirs -name '*.jar' -printf '%p:')"

shift $(($OPTIND - 1))

mkdir -p "$classroot"

srclist=$(mktemp)
find "$src/src" -name '*.java' >$srclist

$JAVA_HOME/bin/javac -cp "$classpath" -sourcepath "$src/src" -d "$classroot" \
    @$srclist
rm $srclist

(cd "$uriroot" && tar c --exclude '*.svn*' . | tar x -C "$dest")

weblib=$dest/WEB-INF/lib
mkdir -p "$weblib"
cat $src/dlinc | copyfiles $root/downloads/output $weblib

webfrag=$(mktemp)

$JAVA_HOME/bin/java -cp "$classpath org.apache.jasper.JspC -d $classroot \
    -p $name -webinc $webfrag -uriroot "$uriroot"

cat $webfrag

rm $webfrag