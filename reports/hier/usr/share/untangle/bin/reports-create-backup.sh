#!/bin/bash

function doHelp() {
    echo "$0 [options]"
    echo "required options: "
    echo " -f output_file   (file to write)"
    echo " -r regex        (regex of table names to backup)"            
    echo "optional options: "
    echo " -h              (help)"
    echo
}

while getopts "r:f:h" opt; do
  case $opt in
    h) doHelp;exit 0;;
    f) FILE=$OPTARG;;
    r) REGEX=$OPTARG;;
  esac
done

if [ -z "$FILE" ] ; then
    doHelp; 
    exit 1;
fi
if [ -z "$REGEX" ] ; then
    doHelp; 
    exit 1;
fi

pg_dump -U postgres -n reports -t "reports.$REGEX" uvm | gzip > $FILE
     
