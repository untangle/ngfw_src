#!/bin/sh

for i in `ls *-transform.version | grep -v template` ; do 
    tran="`echo $i | awk -F\. '{print $1}' `"
    tname="`echo $tran | awk -F- '{print $1}' `"

    echo "Making files for" $tname " ... " 

    for templatefile in `ls template*` ; do 
        newfile="`echo $templatefile | sed -s \"s/template-transform/$tran/\" `"
        echo $newfile
        cat $templatefile | sed -s "s/template-transform/$tran/" | sed -s "s/template/$tname/" > $newfile
    done
done