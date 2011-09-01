echo "Warning: this file needs to be *sourced*"

DIR=$(pwd)

DEFAULT_RUP=${DIR}/../../hades/src
[ ! -d "$DEFAULT_RUP" ] && DEFAULT_RUP=${DIR}/../hades/src

while getopts "u" flag ; do
  if [ "$flag" = "u" ] ; then
    UPDATE=1
  fi
done
shift $((${OPTIND}-1))
 
rup=${1:-$DEFAULT_RUP}

export GTAGSLIBPATH=${DIR}:${rup}

updateTags() {
  cd $1
  /usr/bin/find . -name "*.java" -a ! -path "*/dist/*" -a ! -path '*/downloads/*' -a ! -path '*/staging/*' | gtags -i -f -
  cd $DIR
}

if [ -n "$UPDATE" ] ; then
  echo $GTAGSLIBPATH | perl -pe 's/:/\n/g' | while read gpath ; do
    echo "Updating tags in $gpath"
    updateTags "$gpath"
  done
fi
