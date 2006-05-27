#!/bin/sh

set -e

# functions -------------------------------------------------------------------

function copyfiles()
{
    while read src dest; do
        if [ ! -z "$src" ]; then
            if [ ! -z "$dest" ]; then
                mkdir -p $2/$dest
            fi
            cp -rf $1/$src $2/$dest
        fi
    done
}

# main ------------------------------------------------------------------------

while getopts :c: KEY
  do
  case ${KEY} in
      c) common="$OPTARG";;
  esac
done
shift $(expr $OPTIND - 1)

root=$1
name=$2
src=$3
destroot=$4
dest=$5

uriroot="$src/root"
classroot="$dest/WEB-INF/classes"
webinf="$dest/WEB-INF"
weblib="$webinf/lib"

classpath="$root/alpine/buildtools"

mkdir -p "$dest"

if [ -r $src/dlroot ]; then
    cat $src/dlroot | copyfiles $root/downloads/output $dest
fi

mkdir -p "$weblib"

if [ -r $src/dllib ]; then
    cat $src/dllib | copyfiles $root/downloads/output $weblib
fi

libdirs="$weblib $destroot/usr/share/metavize/lib $destroot/usr/share/java/mvvm $root/downloads/output/jakarta-tomcat-5.0.28-embed/lib"

classpath="$classpath:$JAVA_HOME/lib/tools.jar:$(find $libdirs -name '*.jar' -printf '%p:')"

mkdir -p "$classroot"

srclist=$(mktemp)
find "$src/src" -name '*.java' >$srclist
if grep java $srclist; then
    $JAVA_HOME/bin/javac -cp "$classpath" -sourcepath "$src/src" -d "$classroot" \
        @$srclist
fi
rm $srclist

(cd "$uriroot" && tar c --exclude '*.svn*' . | tar x -C "$dest")
if [ ! -z "$common" ]; then
    (cd "$common/root" && tar c --exclude '*.svn*' . | tar x -C "$dest")
fi

webfrag=$(mktemp)

$JAVA_HOME/bin/java -cp "$classpath" org.apache.jasper.JspC \
    -s -l -v -compile -d $classroot -p $name -webinc $webfrag \
    -uriroot "$dest"

find $classroot -name '*.java' -exec rm '{}' ';'

sed -i "$webinf/web.xml" -e "/@JSP_PRE_COMPILED_SERVLETS@/r $webfrag" \
    -e "/@JSP_PRE_COMPILED_SERVLETS@/d"

rm $webfrag