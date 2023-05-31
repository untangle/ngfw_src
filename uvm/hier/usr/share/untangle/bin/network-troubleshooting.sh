#!/bin/bash
##
## Run troubleshooting commands
##

. /usr/share/untangle/bin/sh_functions.sh

# run_connectivity
#
# Verify basic DNS and TCP conn
#
# Environment variables:
# DNS_TEST_HOST: dns address (e.g.,updates.untangle.com)
# TCP_TEST_HOST: TCP reachable address (e.g.,updates.untangle.com)
run_connectivity(){
    echo -n 'Testing DNS ... '

    success="Successful"
    CMD="dig ${DNS_TEST_HOST} > /dev/null 2>&1"
    if [ $DEBUG -eq 1 ] ; then
        echo
        echo "CMD=$CMD"
        echo
    fi
    eval $CMD
    if [ "$?" = "0" ]; then
        echo "OK"
    else
        echo "FAILED"
        success="Failure"
    fi

    echo -n "Testing TCP Connectivity ... "
    CMD="echo 'GET /' | netcat -q 0 -w 15 ${TCP_TEST_HOST} 80 > /dev/null 2>&1"
    if [ $DEBUG -eq 1 ] ; then
        echo
        echo "CMD=$CMD"
        echo
    fi
    eval $CMD
    if [ "$?" = "0" ]; then 
        echo "OK"
    else
        echo "FAILED"
        success="Failure"
    fi
    echo "Test ${success}!"

}

# run_reachable
#
# Verify an address is icmp reachable
#
# Environment variables:
# HOST: IP address or domain name (e.g.,8.8.8.8)
run_reachable(){
    CMD="ping -c 5 ${HOST}"
    if [ $DEBUG -eq 1 ] ; then
        echo
        echo "CMD=$CMD"
        echo
    fi
    eval $CMD
}

# run_dns
#
# Peform DNS lookup
#
# Environment variables:
# HOST: domain name to lookup (e.g.,www.google.com)
run_dns(){
    CMD="host ${HOST}"
    if [ $DEBUG -eq 1 ] ; then
        echo
        echo "CMD=$CMD"
        echo
    fi
    eval $CMD
    if [ "$?" = "0" ]; then 
        echo "Test Successful"
    else
        echo "Test Failure"
    fi

}

# run_connection
#
# Verify connection by attempting to reach host and port.
#
# Environment variables:
# HOST: domain name to lookup (e.g.,www.google.com)
# HOST_PORT: port (e.g.,80)
run_connection(){
    CMD="echo 1 | netcat -q 0 -v -w 15 ${HOST} ${HOST_PORT}"
    if [ $DEBUG -eq 1 ] ; then
        echo
        echo "CMD=$CMD"
        echo
    fi
    eval $CMD
    if [ "$?" = "0" ]; then
        echo "Test Successful"
    else
        echo "Test Failure"
    fi
}

# run_path
#
# Perform traceroute operation to host.
#
# Environment variables:
# HOST: domain name to lookup (e.g.,www.google.com)
# PROTOCOL: UDP/TCP/ICMP
run_path(){
    CMD="traceroute -${PROTOCOL} ${HOST}"
    if [ $DEBUG -eq 1 ] ; then
        echo
        echo "CMD=$CMD"
        echo
    fi
    eval $CMD
    if [ "$?" = "0" ]; then
        echo "Test Successful"
    else
        echo "Test Failure"
    fi
}

# run_download
#
# Download file
#
# Environment variables:
# URL: URL to download (e.g.,http://cachefly.cachefly.net/5mb.test)
run_download(){
    CMD="wget --output-document=/dev/null ${URL}"
    if [ $DEBUG -eq 1 ] ; then
        echo
        echo "CMD=$CMD"
        echo
    fi
    eval $CMD
}

# run_trace
#
# Perform trace using tcpdump
#
# Environment variables:
# TIMEOUT: How long to run
# MODE: basic or advanced
# TRACE_ARGUMENTS: Advanced specified arguments
# HOST: Basic host 
# HOST_PORT: Basic port
# INTERFACE: Basic interface
# FILENAME: Filename (no path) to use:
run_trace(){

    if [ "${TIMEOUT}" = "" ] ; then
        TIMEOUT=5
    fi

    # Always include for all traces
    TRACE_FIXED_OPTIONS="-U -l -v"

    if [ "${MODE}" == "advanced" ] ; then
        TRACE_ARGUMENTS="$TRACE_FIXED_OPTIONS ${TRACE_ARGUMENTS}"
    else
        TRACE_OVERRIDE_OPTIONS="-n -s 65535"
        if [ "${INTERFACE}" != "" ] && [ "${INTERFACE}" != "null" ] ; then
            TRACE_OVERRIDE_OPTIONS+=" -i ${INTERFACE}"
        fi
        TRACE_OPTIONS="$TRACE_FIXED_OPTIONS $TRACE_OVERRIDE_OPTIONS"
        TRACE_ARGUMENTS=${TRACE_OPTIONS:-}
        TRACE_ARGUMENTS_LIST=()
        if [ "${HOST}" != "" ] && [ "${HOST_PORT}" != "null" ] && [ "${HOST}" != "any" ] ; then
            TRACE_ARGUMENTS_LIST+=("host ${HOST}")
        fi
        if [ "${HOST_PORT}" != "" ] && [ "${HOST_PORT}" != "null" ] ; then
            TRACE_ARGUMENTS_LIST+=("port ${HOST_PORT}")
        fi
        if [ ${#TRACE_ARGUMENTS_LIST[@]} -gt 0 ] ; then
            SEPARATOR=" and"
            TRACE_ARGUMENTS_LIST_EXPANDED=$(printf "$SEPARATOR %s" "${TRACE_ARGUMENTS_LIST[@]}")
            TRACE_ARGUMENTS_LIST_EXPANDED=${TRACE_ARGUMENTS_LIST_EXPANDED:${#SEPARATOR}}
            TRACE_ARGUMENTS+=$TRACE_ARGUMENTS_LIST_EXPANDED
        fi
    fi

    FILENAME="/tmp/network-tests/${FILENAME}"

    CMD="/usr/share/untangle/bin/ut-network-tests-packet.py --timeout ${TIMEOUT} --filename $FILENAME --arguments '$TRACE_ARGUMENTS'"
    if [ $DEBUG -eq 1 ] ; then
        echo
        echo "CMD=$CMD"
        echo
    fi
    eval $CMD

}

$1 "$@"
