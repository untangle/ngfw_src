#!/bin/dash

# This script updates the iptables rules for the untangle-vm
# If it detects the untangle-vm is running it inserts the rules necessary to "redirect" traffic to the UVM
# If it detects the untangle-vm is not running it removes the rules (if they exist)
# If you pass -r as an option it will remove the rules regardless

TUN_DEV=utun
TUN_ADDR="192.0.2.200"
FORCE_REMOVE="false"

MASK_BYPASS=$((0x01000000))
MASK_BOGUS=$((0x80000000)) # unused mark
TCP_REDIRECT_PORTS="9500-9627"

if [ -z "${IPTABLES}" ] ; then
    IPTABLES="/sbin/iptables -w"
fi

## Function to determine the pid of the process that owns the queue
queue_owner()
{
    UVM_PID="invalid"
    
    if [ ! -f /proc/net/netfilter/nfnetlink_queue ] ; then return ; fi

    local t_queue_pid=`awk -v queue=1981 '{ if ( $1 == queue ) print $2 }' /proc/net/netfilter/nfnetlink_queue`
    if [ -z "${t_queue_pid}" ]; then return ; fi
    
    UVM_PID=${t_queue_pid}  
}

## Function to determine if the UVM is running
is_uvm_running()
{
    queue_owner

    if [ "${UVM_PID}x" = "invalidx" ]; then return ; fi

    if [ ! -f "/proc/${UVM_PID}/cmdline" ]; then return ; fi

    grep -q com.untangle.uvm /proc/${UVM_PID}/cmdline 2>| /dev/null  && echo "true"
}

insert_iptables_rules()
{    
    local t_address
    local t_tcp_port_range

    # Do not track output from the UVM 
    # If its UDP or ICMP its part of an existing session, and tracking it will create a new erroneous session
    # If its TCP its non-locally bound and won't go through iptables anyway
    KERNVER=$(uname -r | awk -F. '{ printf("%02d%02d%02d\n",$1,$2,$3); }')
    ORIGVER=30000

    if [ "$KERNVER" -ge "$ORIGVER" ]; then
        ${IPTABLES} -A OUTPUT -t raw -m mark --mark ${MASK_BYPASS}/${MASK_BYPASS} -j CT --notrack -m comment --comment 'CT NOTRACK packets with bypass bit mark set'
    else
        ${IPTABLES} -A OUTPUT -t raw -m mark --mark ${MASK_BYPASS}/${MASK_BYPASS} -j NOTRACK -m comment --comment 'NOTRACK packets with bypass bit mark set'
    fi

    # The UDP packets sent from the UVM seem to have an out intf of the primary wan by default
    # Routing occurs again after OUTPUT chain but only seems to take effect if the packet has been changed
    # This hack toggles one bit on the mark so it has changed which seems to force the re-routing to happen.
    # This is necessary in scenarios where there are multiple independent bridges with WANs in each.
    ${IPTABLES} -A output-untangle-vm -t mangle -p udp -j MARK --set-mark ${MASK_BOGUS}/${MASK_BOGUS} -m comment --comment 'change the mark of all UDP packets to force re-route after OUTPUT'

    # SYN/ACKs will be unmarked by default so we need to restore the connmark so that they will be routed correctly based on the mark
    # This ensures the response goes back out the correct interface 
    ${IPTABLES} -A output-untangle-vm -t mangle -p tcp --tcp-flags SYN,ACK SYN,ACK -m comment --comment 'restore mark on reinject packet' -j restore-interface-marks

    # Redirect any re-injected packets from the TUN interface to us
    ## Add a redirect rule for each address,
    ${IPTABLES} -t nat -N uvm-tcp-redirect >/dev/null 2>&1
    ${IPTABLES} -t nat -F uvm-tcp-redirect >/dev/null 2>&1

    ${IPTABLES} -t tune -N queue-to-uvm >/dev/null 2>&1
    ${IPTABLES} -t tune -F queue-to-uvm >/dev/null 2>&1

    # Insert redirect table in beginning of PREROUTING
    ${IPTABLES} -I PREROUTING -t nat -i ${TUN_DEV} -p tcp -g uvm-tcp-redirect -m comment --comment 'Redirect utun traffic to untangle-vm'

    ${IPTABLES} -A POSTROUTING -t tune -j queue-to-uvm -m comment --comment 'Queue packets to the Untangle-VM'

    # We insert one -j DNAT rule for each local address
    # This is necessary so that the destination address is maintained when replying (with the SYN/ACK)
    # with a regular REDIRECT a random (the first?) address is chosen to reply with (like 192.0.2.200) so for inbound connection the response may go out the wrong WAN
    for t_address in `ip -f inet addr show | awk '/^ *inet/ { sub( "/.*", "", $2 ) ; print $2 }'` ; do
        if [ "${t_address}" = "127.0.0.1" ]; then continue ; fi
        if [ "${t_address}" = "192.0.2.200" ]; then continue ; fi
        ${IPTABLES} -A uvm-tcp-redirect -t nat -i ${TUN_DEV} -t nat -p tcp --destination ${t_address}  -j DNAT --to-destination ${t_address}:${TCP_REDIRECT_PORTS} -m comment --comment "Redirect reinjected packets to ${t_address} to the untangle-vm"
    done
    
    # Redirect TCP traffic to the local ports (where the untangle-vm is listening)
    ${IPTABLES} -A uvm-tcp-redirect -t nat -i ${TUN_DEV} -t nat -p tcp -j REDIRECT --to-ports ${TCP_REDIRECT_PORTS} -m comment --comment 'Redirect reinjected packets to the untangle-vm'

    # Ignore loopback traffic
    ${IPTABLES} -A queue-to-uvm -t tune -i lo -j RETURN -m comment --comment 'Do not queue loopback traffic'
    ${IPTABLES} -A queue-to-uvm -t tune -o lo -j RETURN -m comment --comment 'Do not queue loopback traffic'

    # Ignore traffic that is related to a session we are not watching.
    # If its "related" according to iptables, then original session must have been bypassed
    ${IPTABLES} -A queue-to-uvm -t tune -m conntrack --ctstate RELATED  -j RETURN -m comment --comment 'Do not queue (bypass) sessions related to other bypassed sessions'

    # Ignore traffic that has no conntrack info because we cant NAT it.
    ${IPTABLES} -A queue-to-uvm -t tune -m conntrack --ctstate INVALID  -j RETURN -m comment --comment 'Do not queue (bypass) sessions without conntrack info'

    # Ignore bypassed traffic.
    ${IPTABLES} -A queue-to-uvm -t tune -m mark --mark ${MASK_BYPASS}/${MASK_BYPASS} -j RETURN -m comment --comment 'Do not queue (bypass) all packets with bypass bit set'

    # Queue all of the SYN packets.
    ${IPTABLES} -A queue-to-uvm -t tune -p tcp --syn -j NFQUEUE --queue-num 1981 -m comment --comment 'Queue TCP SYN packets to the untangle-vm'

    # Queue all of the UDP packets.
    ${IPTABLES} -A queue-to-uvm -t tune -m addrtype --dst-type unicast -p udp -j NFQUEUE --queue-num 1982 -m comment --comment 'Queue Unicast UDP packets to the untange-vm'

    # DROP all packets exiting the server with the source address of TUN_ADDR
    # This happens whenever conntrack does not properly remap the reply packet from the redirect
    # I have not been able to figure out the conditions in which this happens, but regardless its pointless to send a packet with this source address
    # as the destination will simply ignore it
    ${IPTABLES} -I queue-to-uvm -t tune -s ${TUN_ADDR} -j DROP -m comment --comment 'Drop unmapped packets leaving server'

    # Redirect packets destined to non-local sockets to local
    ${IPTABLES} -I prerouting-untangle-vm -t mangle -p tcp -m socket -j MARK --set-mark 0xFE00/0xFF00 -m comment --comment "route traffic to non-locally bound sockets to local"
    ${IPTABLES} -I prerouting-untangle-vm -t mangle -p icmp --icmp-type 3/4 -m socket -j MARK --set-mark 0xFE00/0xFF00 -m comment --comment "route ICMP Unreachable Frag needed traffic to local"

    # this is so IPsec/UVM works (Bug #8948)
    ${IPTABLES} -t mangle -A input-untangle-vm -i utun -j MARK --set-mark 0x10000000/0x10000000 -m comment --comment "Set reinjected packet mark"
    
    # Route traffic tagged by previous rule to local
    ip rule del priority 100 >/dev/null 2>&1
    ip rule add priority 100 fwmark 0xFE00/0xFF00 lookup 1000
    ip route add local 0.0.0.0/0 dev lo table 1000 >/dev/null 2>&1 # ignore error if exists

    # Unfortunately we have to give utun an address or the reinjection does not work
    # Use a bogus address
    ifconfig ${TUN_DEV} ${TUN_ADDR} netmask 255.255.255.252
    ifconfig ${TUN_DEV} up

    if [ -f /proc/sys/net/ipv4/conf/${TUN_DEV}/rp_filter ]; then
        echo 0 > /proc/sys/net/ipv4/conf/${TUN_DEV}/rp_filter
    else
        echo "[`date`] ${TUN_DEV} device not exist."
    fi

}

remove_iptables_rules()
{
    ${IPTABLES} -t nat -F uvm-tcp-redirect >/dev/null 2>&1
    ${IPTABLES} -t tune -F queue-to-uvm >/dev/null 2>&1

    KERNVER=$(uname -r | awk -F. '{ printf("%02d%02d%02d\n",$1,$2,$3); }')
    ORIGVER=30000

    if [ "$KERNVER" -ge "$ORIGVER" ]; then
        ${IPTABLES} -D OUTPUT -t raw -m mark --mark ${MASK_BYPASS}/${MASK_BYPASS} -j CT --notrack -m comment --comment 'CT NOTRACK packets with bypass bit mark set' >/dev/null 2>&1
    else
        ${IPTABLES} -D OUTPUT -t raw -m mark --mark ${MASK_BYPASS}/${MASK_BYPASS} -j NOTRACK -m comment --comment 'NOTRACK packets with bypass bit mark set' >/dev/null 2>&1
    fi
    ${IPTABLES} -D output-untangle-vm -t mangle -p udp -j MARK --set-mark ${MASK_BOGUS}/${MASK_BOGUS} -m comment --comment 'change the mark of all UDP packets to force re-route after OUTPUT' >/dev/null 2>&1
    ${IPTABLES} -D input-untangle-vm -t mangle -i utun -j MARK --set-mark 0x10000000/0x10000000 -m comment --comment "Set reinjected packet mark" >/dev/null 2>&1
    ${IPTABLES} -D PREROUTING -t nat -i ${TUN_DEV} -p tcp -g uvm-tcp-redirect -m comment --comment 'Redirect utun traffic to untangle-vm' >/dev/null 2>&1
    ${IPTABLES} -D POSTROUTING -t tune -j queue-to-uvm -m comment --comment 'Queue packets to the Untangle-VM' >/dev/null 2>&1
    ${IPTABLES} -D prerouting-untangle-vm -t mangle -p tcp -m socket -j MARK --set-mark 0xFE00/0xFF00 -m comment --comment "route traffic to non-locally bound sockets to local" >/dev/null 2>&1
    ${IPTABLES} -D prerouting-untangle-vm -t mangle -p icmp --icmp-type 3/4 -m socket -j MARK --set-mark 0xFE00/0xFF00 -m comment --comment "route ICMP Unreachable Frag needed traffic to local" >/dev/null 2>&1
    
    ip rule del priority 100 >/dev/null 2>&1
}

while getopts "r" opt; do
    case $opt in
        r) FORCE_REMOVE="true";;
    esac
done

if [ "$FORCE_REMOVE" = "true" ] ; then
  echo "[`date`] Removing iptables rules."
  remove_iptables_rules
  return 0
fi

if [ "`is_uvm_running`x" = "truex" ] ; then
    echo "[`date`] untangle-vm is running. Inserting iptables rules."
    remove_iptables_rules # just in case
    insert_iptables_rules
else
    echo "[`date`] untangle-vm is not running. Removing iptables rules."
    remove_iptables_rules
fi

return 0


