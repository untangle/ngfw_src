#!/bin/sh


function findInContent() {
  find . -name "*.xml" -exec grep -e ".*\.${1}" '{}' \; | sed 's/^.*figure/figure/' | sed 's/\.png.*$/.png/' | sort -u
}

TEMPFILE=`mktemp`

findInContent png > $TEMPFILE
findInContent jpg >> $TEMPFILE
findInContent gif >> $TEMPFILE
findInContent jpeg >> $TEMPFILE

CONTENT_FILE=`mktemp`

cat $TEMPFILE | sort -u > $CONTENT_FILE

find figure -name "*.png" > $TEMPFILE
find figure -name "*.jpg" >> $TEMPFILE
find figure -name "*.gif" >> $TEMPFILE
find figure -name "*.jpeg" >> $TEMPFILE

FIGURES_FILE=`mktemp`

cat $TEMPFILE | sort -u > $FIGURES_FILE

echo "BEGIN Stuff in figures directory not referenced in content"
grep -v -f $CONTENT_FILE $FIGURES_FILE
echo "ENDOF Stuff in figures directory not referenced in content"

echo "BEGIN Stuff in content without corresponding figure"
grep -v -f $FIGURES_FILE $CONTENT_FILE
echo "ENDOF Stuff in content without corresponding figure"

rm $TEMPFILE
rm $CONTENT_FILE
rm $FIGURES_FILE

#grep -f configtab/outRefs.txt -R --include *.xml --exclude config_*.xml . | sed '/\.\/configtab/ d' > configRefs.txt