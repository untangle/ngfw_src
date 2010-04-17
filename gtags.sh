echo "Warning: this file needs to be *sourced*"

export GTAGSLIBPATH=.:${1:-../../hades/rup}

updateTags() {
  (cd $1 ; /usr/bin/find . -name *.java -a ! -path "*/dist/*" -a ! -path '*/downloads/*' | gtags -i -f -)
}

# FIXME: use getopt
if [ "$2" = "-u" ] || [ "$1" = "-u" ] ; then
  echo $GTAGSLIBPATH | perl -pe 's/:/\n/g' | while read path ; do
    updateTags "$path"
  done
fi
