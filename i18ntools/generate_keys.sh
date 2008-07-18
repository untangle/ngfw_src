#!/bin/sh

ALL_MODULES='main administration system systemInfo mail_casing webfilter virus phish spyware spamassassin shield protofilter'

function update_keys()
{
case "$1" in 
"main")
    cd ../uvm-lib/servlets/webui/po
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../root/script/main.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../root/script/components.js
    msgcat tmp_keys.pot fmt_keys.pot -o tmp_keys.pot
    msgmerge -U ung_main.pot tmp_keys.pot
    rm tmp_keys.pot
    echo 'update po files'
    msgmerge -U ro/ung_main.po ung_main.pot
    ;;
"administration"|"system"|"systemInfo")
    cd ../uvm-lib/servlets/webui/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -k.i18n._ -o tmp_keys.pot ../root/script/config/$1.js
    msgmerge -U ung_$1.pot tmp_keys.pot
    rm tmp_keys.pot
    echo 'update po files'
    msgmerge -U ro/ung_$1.po ung_$1.pot
    ;;
"mail_casing")
    cd ../mail-casing/servlets/quarantine/po/
    xgettext --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../root/quarantine.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../root/remaps.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../root/request.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../root/safelist.js
    msgcat tmp_keys.pot jspx_keys.pot -o tmp_keys.pot
    msgmerge -U ung_$1.pot tmp_keys.pot
    rm tmp_keys.pot
    echo 'update po files'
    msgmerge -U ro/ung_$1.po ung_$1.pot
    ;;    
"webfilter")
    cd ../$1/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -k.i18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/untangle-node-$1/settings.js
    msgcat tmp_keys.pot db_keys.pot block_page_keys.pot -o tmp_keys.pot
    msgmerge -U ung_$1.pot tmp_keys.pot
    rm tmp_keys.pot
    echo 'update po files'
    msgmerge -U ro/ung_$1.po ung_$1.pot
    ;;
"virus")
    cd ../virus-base/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -k.i18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/untangle-base-virus/settings.js
    msgcat tmp_keys.pot db_keys.pot block_page_keys.pot -o tmp_keys.pot
    msgmerge -U ung_$1.pot tmp_keys.pot
    rm tmp_keys.pot
    echo 'update po files'
    msgmerge -U ro/ung_$1.po ung_$1.pot
    ;;
"phish"|"spyware")
    cd ../$1/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -k.i18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/untangle-node-$1/settings.js
    msgcat tmp_keys.pot block_page_keys.pot -o tmp_keys.pot
    msgmerge -U ung_$1.pot tmp_keys.pot
    rm tmp_keys.pot
    echo 'update po files'
    msgmerge -U ro/ung_$1.po ung_$1.pot
    ;;
"spamassassin"|"shield")    
    cd ../$1/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -k.i18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/untangle-node-$1/settings.js
    msgmerge -U ung_$1.pot tmp_keys.pot
    rm tmp_keys.pot
    echo 'update po files'
    msgmerge -U ro/ung_$1.po ung_$1.pot
    ;;
"protofilter")
    cd ../$1/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -k.i18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/untangle-node-$1/settings.js
    msgcat tmp_keys.pot db_keys.pot -o tmp_keys.pot
    msgmerge -U ung_$1.pot tmp_keys.pot
    rm tmp_keys.pot
    echo 'update po files'
    msgmerge -U ro/ung_$1.po ung_$1.pot
    ;;
*)
    echo 1>&2 Module Name \"$1\" is invalid ...
    exit 127
    ;;
esac
}

if [ $# -ne 1 ]; then
     echo 1>&2 Usage: $0 "<module_name | all>"
     exit 127
fi

if [ $1 == 'all' ]
then
    
    current_dir=`pwd`
    for module in ${ALL_MODULES}
    do 
        echo 'Updating keys for '${module}'...'
        update_keys ${module}
        cd ${current_dir}
    done
    
else
    update_keys $1
fi

    
# All done, exit ok
exit 0