#! /bin/bash

# exclude email-node, it is custom
files="`ls *-node-*.postinst | grep -v template`"

for i in $files ; do 
    node="`echo $i | awk -F\. '{print $1}' `"
    nodename="`echo $node | awk -F- '{print $3}' `"

    echo "Making files for" $nodename " ... " 

    for templatefile in `ls untangle-node-template*` ; do 
        newfile="`echo $templatefile | sed -s \"s/template/$nodename/\" `"
        echo $newfile
        cat $templatefile | sed -s "s/template/$nodename/g" > $newfile
    done
done

files="`ls *-casing-*.postinst | grep -v template`"

for i in $files ; do 
    node="`echo $i | awk -F\. '{print $1}' `"
    nodename="`echo $node | awk -F- '{print $3}' `"

    echo "Making files for" $nodename " ... " 

    for templatefile in `ls untangle-casing-template*` ; do 
        newfile="`echo $templatefile | sed -s \"s/template/$nodename/\" `"
        echo $newfile
        cat $templatefile | sed -s "s/template/$nodename/g" > $newfile
    done
done

exit

files="`ls *-base-*.postinst | grep -v template`"

for i in $files ; do 
    node="`echo $i | awk -F\. '{print $1}' `"
    nodename="`echo $node | awk -F- '{print $3}' `"

    echo "Making files for" $nodename " ... " 

    for templatefile in `ls untangle-base-template*` ; do 
        newfile="`echo $templatefile | sed -s \"s/template/$nodename/\" `"
        echo $newfile
        cat $templatefile | sed -s "s/template/$nodename/g" > $newfile
    done
done
