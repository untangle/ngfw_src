#!/bin/sh

set -e

if [ $# -ne 2 ]; then
    echo "usage: $0 destdir patternfile"
    exit -1
fi

mkdir -p $(dirname $2)

echo "running $0 in $1"

(cd $1 && find . -name '*.class' | grep -v '\$' \
    | sed -e 's/\.class$/.java/' -e 's|^./||') > $2