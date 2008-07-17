#!/bin/sh

if [ $# -ne 1 ]; then
     echo 1>&2 Usage: $0 module_name
     exit 127
fi

case "$1" in 
"administration"|"system"|"systemInfo")
    cd ../uvm-lib/servlets/webui/po
    # TODO change charset=utf-8 for pot file
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -k.i18n._ -o tmpkeys.pot ../root/script/config/$1.js
    msgmerge -U ung_$1.pot tmpkeys.pot
    rm tmpkeys.pot
    echo 'update po files'
    msgmerge -U ro/ung_$1.po ung_$1.pot
    ;;
"webfilter")
    cd ../webfilter/po/
    # TODO change charset=utf-8 for pot file
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -k.i18n._ -o tmpkeys.pot ../hier/usr/share/untangle/web/webui/script/untangle-node-webfilter/settings.js
    msgcat tmpkeys.pot db_keys.pot block_page_keys.pot -o tmpkeys.pot
    msgmerge -U ung_webfilter.pot tmpkeys.pot
    rm tmpkeys.pot
    echo 'update po files'
    msgmerge -U ro/ung_webfilter.po ung_webfilter.pot
    ;;
*)
    echo 1>&2 Module Name \"$1\" is invalid ...
    exit 127
    ;;
esac
    
# All done, exit ok
exit 0