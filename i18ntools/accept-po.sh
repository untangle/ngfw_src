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
        untangle-base-virus-blocker)
            d=$work/src/virus-blocker-base
            ;;
        untangle-base-web-filter)
            d=$work/src/web-filter-base
            ;;
        untangle-base-spam-bolcker)
            d=$work/src/spam-blocker-base
            ;;
        untangle-casing-smtp)
            d=$work/src/smtp-casing
            ;;
        untangle-install-wizard)
            d=$work/src/gui
            ;;
        untangle-libuvm)
            d=$work/src/uvm
            ;;
        untangle-node-ad-blocker)
            d=$work/src/ad-blocker
            ;;
        untangle-node-directory-connector)
            d=$hades/src/directory-connector
            ;;
        untangle-node-bandwidth-control)
            d=$hades/src/bandwidth
            ;;
        untangle-node-configuration-backup)
            d=$hades/src/configuration-backup
            ;;
        untangle-node-branding-manager)
            d=$hades/src/branding
            ;;
        untangle-node-spam-blocker)
            d=$hades/src/spam-blocker
            ;;
        untangle-node-virus-blocker)
            d=$hades/src/virus-blocker
            ;;
        untangle-node-wan-failover)
            d=$hades/src/wan-failover
            ;;
        untangle-node-firewall)
            d=$work/src/firewall
            ;;
        untangle-node-ips)
            d=$work/src/ips
            ;;
            ;;
        untangle-node-license)
            d=$hades/src/license
            ;;
        untangle-node-openvpn)
            d=$work/src/openvpn
            ;;
        untangle-node-phish-blocker)
            d=$work/src/phish-blocker
            ;;
        untangle-node-policy-manager)
            d=$hades/src/policy
            ;;
        untangle-node-application-control-lite)
            d=$work/src/application-control-lite
            ;;
        untangle-node-reports)
            d=$work/src/reporting
            ;;
        untangle-node-shield)
            d=$work/src/shield
            ;;
        untangle-node-web-filter)
            d=$hades/src/web-filter
            ;;
        untangle-node-spam-blocker-lite)
            d=$work/src/spam-blocker-lite
            ;;
        untangle-node-wan-balancer)
            d=$hades/src/wan-balancer
            ;;
        untangle-node-live-support)
            d=$hades/src/support
            ;;
        untangle-node-web-cache)
            d=$hades/src/web-cache
            ;;
        untangle-node-ipsec-vpn)
            d=$hades/src/ipsec-vpn
            ;;
        untangle-node-application-control)
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
