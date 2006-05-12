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
webinf="$dest/WEB-INF"
weblib="$webinf/lib"

libdirs="$root/alpine/output/usr/share/metavize/lib $root/alpine/output/usr/share/java/mvvm $root/downloads/output/jakarta-tomcat-5.0.28-embed/lib"

classpath="$JAVA_HOME/lib/tools.jar:$(find $libdirs -name '*.jar' -printf '%p:')"

shift $(($OPTIND - 1))

mkdir -p "$classroot"

srclist=$(mktemp)
find "$src/src" -name '*.java' >$srclist

$JAVA_HOME/bin/javac -cp "$classpath" -sourcepath "$src/src" -d "$classroot" \
    @$srclist
rm $srclist

(cd "$uriroot" && tar c --exclude '*.svn*' . | tar x -C "$dest")

mkdir -p "$weblib"
cat $src/dlinc | copyfiles $root/downloads/output $weblib

webfrag=$(mktemp)

$JAVA_HOME/bin/java -cp "$classpath" org.apache.jasper.JspC -s -l -v -compile \
    -d $classroot -p $name -webinc $webfrag -uriroot "$uriroot"

find $classroot -name '*.java' -exec rm '{}' ';'

sed -i "$webinf/web.xml" -e "/@JSP_PRE_COMPILED_SERVLETS@/r $webfrag" \
    -e "/@JSP_PRE_COMPILED_SERVLETS@/d"

rm $webfrag