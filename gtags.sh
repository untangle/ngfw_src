echo "Warning: this file needs to be *sourced*"

export GTAGSLIBPATH=../../hades/rup

if [ $# -gt 0 ] ; then
  /usr/bin/find . -name *.java -a ! -path "*/dist/*" -a ! -path '*/downloads/*' | gtags -i -f -
  (cd $GTAGSLIBPATH ; /usr/bin/find . -name *.java -a ! -path "*/dist/*" -a ! -path '*/downloads/*' | gtags -i -f -)
fi
