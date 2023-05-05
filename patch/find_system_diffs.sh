#!/bin/bash

ARG_INDEX=1

FILES=()
while true; do
	eval "MATCH=\${$ARG_INDEX}"
	if [ "$MATCH" = "" ] ; then
		break
        fi
        ARG_INDEX=$((ARG_INDEX+1))

	if [ "$MATCH" = "all" ] ; then
			echo "All diffs:"
			MATCH=.
	else
			echo "Diffs containing '$MATCH':"
	fi


	OLD_IFS=$IFS
	IFS=$'\n'
	for line in `diff -qr /root/orig/usr /usr 2>|/dev/null \
		| grep -v changelog \
		| grep -v .jar \
		| grep -v .png \
		| grep -v web.xml \
		| grep -e "$MATCH"`; do
		echo $line
                if [[ $line == Only* ]] ; then
			# Only exists in new (presumably!)
			path=${line%:*}
			path=${path#*/}
			filename=${line#*: }
			FILES+=("/$path/$filename")
                else
			# Get new file
			FILES+=($(echo $line | cut -d' ' -f4))
                fi
	done
	IFS=$OLD_IFS
done

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
