#!/bin/bash

function doHelp() {
    echo "$0 [options]"
    echo "required options: "
    echo " -f input_file   (file to write)"
    echo "optional options: "
    echo " -h              (help)"
    echo
}

while getopts "f:h" opt; do
  case $opt in
    h) doHelp;exit 0;;
    f) FILE=$OPTARG;;
  esac
done

if [ -z "$FILE" ] ; then
    doHelp; 
    exit 1;
fi

zcat $FILE | psql -U postgres uvm 2>&1 | grep -v 'already exists'
     
