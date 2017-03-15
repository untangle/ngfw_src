#!/bin/bash

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
        virus-blocker-base)
            d=$work/src/virus-blocker-base
            ;;
        web-filter-base)
            d=$work/src/web-filter-base
            ;;
        spam-bolcker-base)
            d=$work/src/spam-blocker-base
            ;;
        smtp)
            d=$work/src/smtp-casing
            ;;
        untangle-install-wizard)
            d=$work/src/gui
            ;;
        untangle-libuvm)
            d=$work/src/uvm
            ;;
        ad-blocker)
            d=$work/src/ad-blocker
            ;;
        directory-connector)
            d=$hades/src/directory-connector
            ;;
        bandwidth-control)
            d=$hades/src/bandwidth
            ;;
        configuration-backup)
            d=$hades/src/configuration-backup
            ;;
        branding-manager)
            d=$hades/src/branding
            ;;
        spam-blocker)
            d=$hades/src/spam-blocker
            ;;
        virus-blocker)
            d=$hades/src/virus-blocker
            ;;
        wan-failover)
            d=$hades/src/wan-failover
            ;;
        firewall)
            d=$work/src/firewall
            ;;
        ips)
            d=$work/src/ips
            ;;
            ;;
        license)
            d=$hades/src/license
            ;;
        openvpn)
            d=$work/src/openvpn
            ;;
        phish-blocker)
            d=$work/src/phish-blocker
            ;;
        policy-manager)
            d=$hades/src/policy
            ;;
        application-control-lite)
            d=$work/src/application-control-lite
            ;;
        reports)
            d=$work/src/reporting
            ;;
        shield)
            d=$work/src/shield
            ;;
        web-filter)
            d=$hades/src/web-filter
            ;;
        spam-blocker-lite)
            d=$work/src/spam-blocker-lite
            ;;
        wan-balancer)
            d=$hades/src/wan-balancer
            ;;
        live-support)
            d=$hades/src/support
            ;;
        web-cache)
            d=$hades/src/web-cache
            ;;
        ipsec-vpn)
            d=$hades/src/ipsec-vpn
            ;;
        application-control)
            d=$hades/src/application-control
            ;;
        untangle-system-stats*)
            d=$internal/isotools/installer-pkgs-additional/untangle-system-stats/debian
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
