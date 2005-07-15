#!/bin/sh

# exclude email-transform, it is custom
files="`ls *-transform.version | grep -v template | grep -v email-transform`"

for i in $files ; do 
    tran="`echo $i | awk -F\. '{print $1}' `"
    tname="`echo $tran | awk -F- '{print $1}' `"

    echo "Making files for" $tname " ... " 

    for templatefile in `ls template-transform*` ; do 
        newfile="`echo $templatefile | sed -s \"s/template-transform/$tran/\" `"
        echo $newfile
        cat $templatefile | sed -s "s/template-transform/$tran/" | sed -s "s/template/$tname/" > $newfile
    done
done


files="`ls *-casing.version | grep -v template`"

for i in $files ; do 
    tran="`echo $i | awk -F\. '{print $1}' `"
    tname="`echo $tran | awk -F- '{print $1}' `"

    echo "Making files for" $tname " ... " 

    for templatefile in `ls template-casing*` ; do 
        newfile="`echo $templatefile | sed -s \"s/template-casing/$tran/\" `"
        echo $newfile
        cat $templatefile | sed -s "s/template-transform/$tran/" | sed -s "s/template/$tname/" > $newfile
    done
done
