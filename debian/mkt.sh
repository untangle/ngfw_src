#!/bin/sh

# exclude email-node, it is custom
files="`ls *-node-*.postinst | grep -v template`"

for i in $files ; do 
    node="`echo $i | awk -F\. '{print $1}' `"
    nodename="`echo $node | awk -F- '{print $2}' `"

    echo "Making files for" $nodename " ... " 

    for templatefile in `ls template-node*` ; do 
        newfile="`echo $templatefile | sed -s \"s/template-node/$node/\" `"
        echo $newfile
        cat $templatefile | sed -s "s/template-node/$node/" | sed -s "s/template/$nodename/" > $newfile
    done
done


files="`ls *-node-*.postinst | grep -v template`"

for i in $files ; do 
    node="`echo $i | awk -F\. '{print $1}' `"
    nodename="`echo $node | awk -F- '{print $2}' `"

    echo "Making files for" $nodename " ... " 

    for templatefile in `ls template-casing*` ; do 
        newfile="`echo $templatefile | sed -s \"s/template-casing/$node/\" `"
        echo $newfile
        cat $templatefile | sed -s "s/template-node/$node/" | sed -s "s/template/$nodename/" > $newfile
    done
done


files="`ls *-node-*.postinst | grep -v template`"

for i in $files ; do 
    node="`echo $i | awk -F\. '{print $1}' `"
    nodename="`echo $node | awk -F- '{print $2}' `"

    echo "Making files for" $nodename " ... " 

    for templatefile in `ls template-base*` ; do 
        newfile="`echo $templatefile | sed -s \"s/template-base/$node/\" `"
        echo $newfile
        cat $templatefile | sed -s "s/template-node/$node/" | sed -s "s/template/$nodename/" > $newfile
    done
done
