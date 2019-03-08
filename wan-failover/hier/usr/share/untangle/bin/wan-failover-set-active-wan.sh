#!/bin/dash

UNTANGLE_PRIORITY_BASE="70"
UNTANGLE_PRIORITY_DEFAULT="1000000"


if [ $# -lt 3 ] ; then
    echo "usage: $0 <interfaceId> <uplinksOnline> <uplinksOffline>"
    exit 1
fi
DEFAULT_WAN=$1
ONLINE_WANS=$2
OFFLINE_WANS=$3

# This remove the ip fwmark rules for any WAN considered down
# This is so the route rules won't route to down WANs
# It also re-adds any ip fwmark rules for any up WANs just in case
# they were previously considered down
update_ip_route_rules()
{
    for t_uplink in ${OFFLINE_WANS} ${ONLINE_WANS}; do
        ip rule del priority ${UNTANGLE_PRIORITY_BASE}`printf "%03i" $t_uplink` 2>&1 | grep -v 'No such file'
    done

    for t_uplink in ${ONLINE_WANS} ; do
        ip rule add priority ${UNTANGLE_PRIORITY_BASE}`printf "%03i" $t_uplink` `printf "fwmark 0x%02x00/0xff00" $t_uplink` lookup uplink.$t_uplink 
    done
}

update_default_link_rule()
{
    ## Delete the default rules (ip lets you have two rules with the same priority)
    for t in `ip rule show | awk "/^${UNTANGLE_PRIORITY_DEFAULT}/ { print \"a\" }"`; do
        ip rule del priority ${UNTANGLE_PRIORITY_DEFAULT}
    done
    
    printf "[`date`] Setting default to uplink.%d\n" ${DEFAULT_WAN}
    
    ## Use printf to get rid of the spacing
    ip rule add priority ${UNTANGLE_PRIORITY_DEFAULT} lookup `printf "uplink.%d" ${DEFAULT_WAN}`
}

update_dns_servers()
{
    local t_uplink
    local t_offline_links
    local t_online_links

    local t_temp
    local t_temp_2

    t_temp=`mktemp`

    for t_uplink in ${ONLINE_WANS} ${DEFAULT_WAN} ; do
        if [ -n "${t_online_links}" ]; then t_online_links="${t_online_links}|" ; fi
        t_online_links="${t_online_links}uplink.${t_uplink}"
    done
        
    for t_uplink in ${OFFLINE_WANS} ; do
        if [ -n "${t_offline_links}" ]; then t_offline_links="${t_offline_links}|" ; fi
        t_offline_links="${t_offline_links}uplink.${t_uplink}"
    done

    if [ -z "${t_online_links}" ]; then
        t_online_links="this_will_never_match"
    fi
    
    if [ -z "${t_offline_links}" ]; then
        t_offline_links="this_will_never_match"
    fi

    awk '/^(# *)?server=.*('${t_online_links}')$/ { sub( /^(# *)?/, "" ) ; print ; next } ; /^(# *)?server=.*('${t_offline_links}')$/ { sub( /^(# *)?/, "# " ) ; print ; next } ;  { print } ' /etc/dnsmasq.conf > ${t_temp}

    ## If all of the DNS servers are disabled, enabled the first one.
    grep -q "^server=" /etc/dnsmasq.conf > /dev/null 2>&1 || {
        echo "[`date`] All DNS servers are disabled, enabling first server."

        t_temp_2=`mktemp`

        awk -v p=false '/^(# *)?server=.*$/ { if ( p == "false" ) { sub( /^(# *)?/, "" ) } ; print ; p="true"; next } ; { print } ' ${t_temp} > ${t_temp_2}
        mv ${t_temp_2} ${t_temp}
    }

    diff -q ${t_temp} /etc/dnsmasq.conf > /dev/null
    if [ $? -eq 0 ] ; then
        echo "[`date`] DNS configuration is up-to-date."
    else
        echo "[`date`] DNS configuration updated."

        mv ${t_temp} /etc/dnsmasq.conf
        chmod 664 /etc/dnsmasq.conf
        systemctl restart dnsmasq
    fi
    
    rm -f ${t_temp}
}

## Start of script

if [ -z "${DEFAULT_WAN}" ] || [ "${DEFAULT_WAN}" = "0" ]; then
    echo "[DEBUG:`date`] No WANs are active, using first available link"
    DEFAULT_WAN=`printf "%s\n" ${WAN_FAILOVER_UPLINKS} | head -n 1`
fi

if [ -z "${DEFAULT_WAN}" ] ; then
    echo "[DEBUG:`date`] No uplinks, unable to bring up link."
    exit 0
fi

echo "[`date`] Default WAN : ${DEFAULT_WAN}"
echo "[`date`] Live    WANs: ${ONLINE_WANS}"
echo "[`date`] Dead    WANs: ${OFFLINE_WANS}"

update_ip_route_rules
update_default_link_rule 
update_dns_servers 

# tell splitd to update its balance routes (if its running)
if [ -x /etc/untangle/post-network-hook.d/040-splitd ] ; then
    echo "[`date`] Inserting Balancing rules..."
    /etc/untangle/post-network-hook.d/040-splitd
fi
# tell wan-balancer to update its balance routes (if its running)
if [ -x /etc/untangle/post-network-hook.d/040-wan-balancer ] ; then
    echo "[`date`] Inserting Balancing rules..."
    /etc/untangle/post-network-hook.d/040-wan-balancer
fi


# flush cache
ip route flush cache

/bin/true
