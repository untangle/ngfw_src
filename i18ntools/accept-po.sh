#!/bin/sh

if [ $# -ne 4 ]; then
    echo "usage $0 po.zip work hades internal"
    exit -1
fi

po="$1"
work="$2"
hades="$3"
internal="$4"

t=$(mktemp -d)
unzip -d $t "$po"

for f in $(find $t -type f -name '*.po'); do
    c=$(basename $(dirname $f))
    m=$(basename $f .po)

    d=""

    case $m in
        untangle-apache2-config)
            d=$work/pkgs/untangle-apache2-config
            ;;
        untangle-base-virus)
            d=$work/src/virus-base
            ;;
        untangle-base-webfilter)
            d=$work/src/webfilter-base
            ;;
        untangle-casing-mail)
            d=$work/src/mail-casing
            ;;
        untangle-install-wizard)
            d=$work/src/gui
            ;;
        untangle-libuvm)
            d=$work/src/uvm-lib
            ;;
        untangle-node-adblocker)
            d=$work/src/adblocker
            ;;
        untangle-node-adconnector)
            d=$hades/rup/adconnector
            ;;
        untangle-node-boxbackup)
            d=$hades/rup/boxbackup
            ;;
        untangle-node-firewall)
            d=$work/src/firewall
            ;;
        untangle-node-ips)
            d=$work/src/ips
            ;;
        untangle-node-openvpn)
            d=$work/src/openvpn
            ;;
        untangle-node-pcremote)
            d=$hades/rup/pcremote
            ;;
        untangle-node-phish)
            d=$work/src/phish
            ;;
        untangle-node-portal)
            d=$hades/rup/portal
            ;;
        untangle-node-protofilter)
            d=$work/src/protofilter
            ;;
        untangle-node-reporting)
            d=$work/src/reporting
            ;;
        untangle-node-shield)
            d=$work/src/shield
            ;;
        untangle-node-spamassassin)
            d=$work/src/spamassassin
            ;;
        untangle-node-spyware)
            d=$work/src/spyware
            ;;
        untangle-net-alpaca)
            d=$work/pkgs/untangle-net-alpaca
            ;;
        untangle-systray)
            d=$internal/isotools/wintangle-installer
            ;;
        untangle-system-stats*)
            d=$internal/isotools/installer-pkgs-additional/untangle-system-stats/debian
            ;;
        untangle-systray)
            d=$internal/isotools/wintangle-systray
            ;;
        wintangle-installer)
            d=$internal/isotools/wintangle-installer/
            ;;
        *)
            echo "unknown module: $m"
    esac

    if [ "x$d" != "x" ]; then
        i="$d/po/$c/"
        mkdir -p $i
        cp $f $i
        svn add $i
    fi
done

echo "remember to run svn commit in $work, $hades, and $internal"