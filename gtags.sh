echo "Warning: this file needs to be *sourced*"

DEFAULT_RUP=../../hades/src
[ ! -d "$DEFAULT_RUP" ] && DEFAULT_RUP=../hades/src

while getopts "u" flag ; do
  if [ "$flag" = "u" ] ; then
    UPDATE=1
  fi
done
shift $((${OPTIND}-1))
 
rup=${1:-$DEFAULT_RUP}

export GTAGSLIBPATH=.:${rup}

updateTags() {
  pushd $1
  /usr/bin/find . -name *.java -a ! -path "*/dist/*" -a ! -path '*/downloads/*' | gtags -i -f -
  popd
}

if [ -n "$UPDATE" ] ; then
  echo $GTAGSLIBPATH | perl -pe 's/:/\n/g' | while read path ; do
    updateTags "$path"
  done
fi
