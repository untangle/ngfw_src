#!/bin/dash

# This script updates the iptables rules for the untangle-vm
# If it detects the untangle-vm is running it inserts the rules necessary to "redirect" traffic to the UVM
# If it detects the untangle-vm is not running it removes the rules (if they exist)

TUN_DEV=utun
TUN_ADDR="192.0.2.42"

MASK_BYPASS=$((0x01000000))
TCP_REDIRECT_PORTS="9500-9627"

iptables_debug()
{
   echo "[`date`] /sbin/iptables $@"
   /sbin/iptables "$@"
}

iptables_debug_onerror()
{
    # Ignore -N errors
    /sbin/iptables "$@" || {
        [ "${3}x" != "-Nx" ] && echo "[`date`] Failed: /sbin/iptables $@"
    }

    true
}

if [ -z "${IPTABLES}" ] ; then
    IPTABLES=iptables
fi

## Function to determine the pid of the process that owns the queue
queue_owner()
{
    UVM_PID="invalid"
    
    if [ ! -f /proc/net/netfilter/nfnetlink_queue ] ; then return ; fi

    local t_queue_pid=`awk -v queue=0 '{ if ( $1 == queue ) print $2 }' /proc/net/netfilter/nfnetlink_queue`
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
    ${IPTABLES} -A OUTPUT -t raw -m mark --mark ${MASK_BYPASS}/${MASK_BYPASS} -j NOTRACK -m comment --comment 'NOTRACK packets with bypass bit mark set'

    # Redirect any re-injected packets from the TUN interface to us
    ## Add a redirect rule for each address,
    ${IPTABLES} -t nat -N uvm-tcp-redirect >/dev/null 2>&1
    ${IPTABLES} -t nat -F uvm-tcp-redirect >/dev/null 2>&1

    ${IPTABLES} -t tune -N queue-to-uvm >/dev/null 2>&1
    ${IPTABLES} -t tune -F queue-to-uvm >/dev/null 2>&1

    # Insert redirect table in beginning of PREROUTING
    ${IPTABLES} -I PREROUTING -t nat -i ${TUN_DEV} -p tcp -g uvm-tcp-redirect -m comment --comment 'Redirect utun traffic to untangle-vm'

    ${IPTABLES} -I POSTROUTING -t tune -j queue-to-uvm -m comment --comment 'Queue packets to the Untangle-VM'

    # We insert one -j DNAT rule for each local address
    # This is necessary so that the destination address is maintained when replying (with the SYN/ACK)
    # with a regular REDIRECT a random (the first?) address is chosen to reply with (like 192.0.2.43) so for inbound connection the response may go out the wrong WAN
    for t_address in `ip -f inet addr show | awk '/^ *inet/ { sub( "/.*", "", $2 ) ; print $2 }'` ; do
        if [ "${t_address}" = "127.0.0.1" ]; then continue ; fi
        if [ "${t_address}" = "192.0.2.42" ]; then continue ; fi
        if [ "${t_address}" = "192.0.2.43" ]; then continue ; fi
        ${IPTABLES} -A uvm-tcp-redirect -t nat -i ${TUN_DEV} -t nat -p tcp --destination ${t_address}  -j DNAT --to-destination ${t_address}:${TCP_REDIRECT_PORTS} -m comment --comment "Redirect reinjected packets to ${t_address} to the untangle-vm"
    done
    
    # Redirect TCP traffic to the local ports (where the untangle-vm is listening)
    ${IPTABLES} -A uvm-tcp-redirect -t nat -i ${TUN_DEV} -t nat -p tcp -j REDIRECT --to-ports ${TCP_REDIRECT_PORTS} -m comment --comment 'Redirect reinjected packets to the untangle-vm'

    ## Guard the ports (this part uses : not -)
    ## FIXME move this to packet filter
    # t_tcp_port_range=`echo ${TCP_REDIRECT_PORTS} | sed 's|-|:|'`
    # ${IPTABLES} -t filter -I INPUT 1 ! -i utun -p tcp --destination-port ${t_tcp_port_range} -m conntrack --ctstate NEW,INVALID -j DROP -m comment --comment 'Drop traffic for untangle-vm listen ports except for redirected traffic"'

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
    ${IPTABLES} -A queue-to-uvm -t tune -p tcp --syn -j NFQUEUE -m comment --comment 'Queue TCP SYN packets to the untangle-vm'

    # Queue all of the UDP packets.
    ${IPTABLES} -A queue-to-uvm -t tune -m addrtype --dst-type unicast -p udp -j NFQUEUE -m comment --comment 'Queue Unicast UDP packets to the untange-vm'

    # Unfortunately we have to give utun an address or the reinjection does not work
    # Use a bogus address
    ifconfig ${TUN_DEV} ${TUN_ADDR} netmask 255.255.255.0 
    ifconfig ${TUN_DEV} up

    if [ -f /proc/sys/net/ipv4/conf/${TUN_DEV}/rp_filter ]; then
        echo 0 > /proc/sys/net/ipv4/conf/${TUN_DEV}/rp_filter
    else
        echo "[`date`] ${TUN_DEV} device not exist."
    fi

    ## Ignore any traffic that is on the utun interface
    ## FIXME move to packet filter
    # ${IPTABLES} -t mangle -I packet-filter-rules 1 -i ${TUN_DEV} -j RETURN -m comment --comment "Allow all traffic on utun interface"

}

remove_iptables_rules()
{
    ${IPTABLES} -t nat -F uvm-tcp-redirect >/dev/null 2>&1
    ${IPTABLES} -t tune -F queue-to-uvm >/dev/null 2>&1

    ${IPTABLES} -D OUTPUT -t raw -m mark --mark ${MASK_BYPASS}/${MASK_BYPASS} -j NOTRACK -m comment --comment 'NOTRACK packets with bypass bit mark set' >/dev/null 2>&1
    ${IPTABLES} -D PREROUTING -t nat -i ${TUN_DEV} -p tcp -g uvm-tcp-redirect -m comment --comment 'Redirect utun traffic to untangle-vm' >/dev/null 2>&1
    ${IPTABLES} -D POSTROUTING -t tune -j queue-to-uvm -m comment --comment 'Queue packets to the Untangle-VM' >/dev/null 2>&1
}

rules_already_present()
{
    # check two rules, if both exist, all rules probably exist
    echo false
    #iptables -t raw -nvL OUTPUT | grep -q NOTRACK && iptables -t tune -nvL OUTPUT | grep -q NFQUEUE && echo "true"
}

if [ "`is_uvm_running`x" = "truex" ]; then
    if [ "`rules_already_present`x" = "truex" ]; then
        echo "[`date`] The untangle-vm is running. Rules already exist. Doing nothing. "
    else
        echo "[`date`] The untangle-vm is running. Inserting iptables rules ... "
        remove_iptables_rules # just in case
        insert_iptables_rules
        echo "[`date`] The untangle-vm is running. Inserting iptables rules ... done"
    fi
else
  echo "[`date`] The untangle-vm is not running. Removing iptables rules ..."
  remove_iptables_rules
  echo "[`date`] The untangle-vm is not running. Removing iptables rules ... done"
fi

return 0


