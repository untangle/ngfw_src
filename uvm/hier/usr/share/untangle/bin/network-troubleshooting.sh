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
# DNS_TEST_HOST: dns address (e.g.,updates.edge.arista.com)
# TCP_TEST_HOST: TCP reachable address (e.g.,updates.edge.arista.com)
run_connectivity(){
    echo -n 'Testing DNS ... '

    local success="Successful"
    if dig "${DNS_TEST_HOST}" > /dev/null 2>&1 ; then
        echo "OK"
    else
        echo "FAILED"
        success="Failure"
    fi

    echo -n "Testing TCP Connectivity ... "
    # Using an array for command to avoid shell splitting/injection
    local netcat_cmd=("netcat" "-q" "0" "-w" "15" "${TCP_TEST_HOST}" "80")
    if echo 'GET /' | "${netcat_cmd[@]}" > /dev/null 2>&1 ; then
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
    # Execute ping directly with quoted variable
    ping -c 5 "${HOST}"
}

# run_dns
#
# Peform DNS lookup
#
# Environment variables:
# HOST: domain name to lookup (e.g.,www.google.com)
run_dns(){
    # Execute host directly with quoted variable
    if host "${HOST}" ; then
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
    # Using an array for command to avoid shell splitting/injection
    local netcat_cmd=("netcat" "-q" "0" "-v" "-w" "15" "${HOST}" "${HOST_PORT}")
    if echo 1 | "${netcat_cmd[@]}" ; then
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
    # Execute traceroute directly with quoted variables
    if traceroute "-${PROTOCOL}" "${HOST}" ; then
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
    if [ $DEBUG -eq 1 ] ; then
        echo
        echo "CMD=wget --output-document=/dev/null \"${URL}\""
        echo
    fi
    # Execute wget directly with quoted URL to prevent command injection
    wget --output-document=/dev/null "${URL}"
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

# Function to sanitize filename, remove path traversal elements
sanitize_filename() {
    local filename="$1"
    # Remove any directory traversal sequences
    filename="${filename//\.\.\//}" # Replace occurrences of "../"
    filename="${filename//\.\//}"   # Replace occurrences of "./"
    # Ensure it's just a basename to prevent creating files in subdirectories
    filename=$(basename "${filename}")
    echo "${filename}"
}

run_trace(){
    if [ "${TIMEOUT}" = "" ] ; then
        TIMEOUT=5
    fi

    # Always include for all traces
    local TRACE_FIXED_OPTIONS="-U -l -v"
    local full_trace_arguments=""
    
    if [ "${MODE}" == "advanced" ] ; then
        full_trace_arguments="${TRACE_FIXED_OPTIONS} ${TRACE_ARGUMENTS}"
    else
        local TRACE_OVERRIDE_OPTIONS="-n -s 65535"
        if [ "${INTERFACE}" != "" ] && [ "${INTERFACE}" != "null" ] ; then
            TRACE_OVERRIDE_OPTIONS+=" -i ${INTERFACE}"
        fi
        
        full_trace_arguments="${TRACE_FIXED_OPTIONS} ${TRACE_OVERRIDE_OPTIONS}"
        
        local TRACE_ARGUMENTS_LIST=()
        # Safely quote HOST and HOST_PORT for tcpdump filter syntax
        if [ "${HOST}" != "" ] && [ "${HOST_PORT}" != "null" ] && [ "${HOST}" != "any" ] ; then
            TRACE_ARGUMENTS_LIST+=("host $(printf %q "${HOST}")")
        fi
        if [ "${HOST_PORT}" != "" ] && [ "${HOST_PORT}" != "null" ] ; then
            TRACE_ARGUMENTS_LIST+=("port $(printf %q "${HOST_PORT}")")
        fi
        
        if [ ${#TRACE_ARGUMENTS_LIST[@]} -gt 0 ]; then
            local IFS=" and " # Internal Field Separator for joining array elements
            full_trace_arguments+=" ${TRACE_ARGUMENTS_LIST[*]}"
        fi
    fi

    # Sanitize FILENAME to prevent path traversal
    local SANITIZED_FILENAME=$(sanitize_filename "${FILENAME}")
    local FULL_PATH_FILENAME="/tmp/network-tests/${SANITIZED_FILENAME}"

    # Assuming ut-network-tests-packet.py correctly handles the 'arguments' string for tcpdump.
    /usr/share/untangle/bin/ut-network-tests-packet.py --timeout "${TIMEOUT}" --filename "${FULL_PATH_FILENAME}" --arguments "${full_trace_arguments}"
}

$1 "$@"
