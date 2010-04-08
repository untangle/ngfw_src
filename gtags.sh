echo "Warning: this file needs to be *sourced*"

export GTAGSLIBPATH=../../hades/rup

updateTags() {
  (cd $1 ; /usr/bin/find . -name *.java -a ! -path "*/dist/*" -a ! -path '*/downloads/*' | gtags -i -f -)
}

if [ $# -gt 0 ] ; then
  # FIXME: split GTAGSLIBPATH on '.', and call updateTags on every
  # component
  updateTags .
  updateTags $GTAGSLIBPATH
fi
