#!/bin/bash

MATCH=$1

if [ "$MATCH" = "" ] ; then
        echo "All diffs:"
        MATCH=.
else
        echo "Diffs containing '$MATCH':"
fi

FILES=()

OLD_IFS=$IFS
IFS=$'\n'
for line in `diff -qr /root/orig/usr /usr 2>|/dev/null \
	| grep -v changelog \
	| grep -v .jar \
	| grep -v .png \
	| grep -v web.xml \
	| grep -e "$MATCH"`; do
	echo $line
	FILES+=($(echo $line | cut -d' ' -f4))
done
IFS=$OLD_IFS

echo
echo "Live files:"
for file in "${FILES[@]}"; do
	echo $file
done

echo
echo "Recommended commands:"
echo "$ tar -czf patch.tgz ${FILES[@]}"
echo "$ create_patch.sh"
echo
