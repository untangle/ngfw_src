#!/bin/dash

## Print the status of a list of interfaces.  Each interface should be
## given on the command line.

## Return syntax:
## <interface name>: <connected|disconnected|unknown> <10|100|unknown> <full-duplex|half-duplex|unknown>

FLAG_CONNECTED=connected
FLAG_DISCONNECTED=disconnected
FLAG_UNKNOWN=unknown

FLAG_SPEED_100=100
FLAG_SPEED_10=10

FLAG_DUPLEX_FULL=full-duplex
FLAG_DUPLEX_HALF=half-duplex

interfaceList=$*

function getInterfaceStatus()
{
    local t_intf=$1
    local t_isConnected=${FLAG_UNKNOWN}
    local t_speed=${FLAG_UNKNOWN}
    local t_duplex=${FLAG_UNKNOWN}

    t_status=`ethtool ${t_intf} | grep -v Supported | egrep '(Speed|Duplex|Link detected)' | sed -e '{:start;N;s/\n/ /g;t start}'`
    if [ -n "${t_status}" ]; then
        ## Check for link
        if [ "${t_status/Link detected: yes}" != "${t_status}" ]; then
            t_isConnected="${FLAG_CONNECTED}"
        
            if [ "${t_status/Speed: }" != "${t_status}" ]; then
                t_speed=`echo ${t_status} | cut -d " " -f 2`
            fi
            
            if [ "${t_status/Duplex: Full}" != "${t_status}" ]; then
                t_duplex="${FLAG_DUPLEX_FULL}"
            elif [ "${t_status/Duplex: Half}" != "${t_status}" ]; then
                t_duplex="${FLAG_DUPLEX_HALF}"
            fi
        elif [ "${t_status/Link detected: no}" != "${t_status}" ]; then
            t_isConnected="${FLAG_DISCONNECTED}"
        fi
    else
        ##  Use mii-tool instead
        local t_status=`mii-tool ${t_intf}`
        if [ "${t_status%link ok}" != "${t_status}" ]; then
            t_isConnected="${FLAG_CONNECTED}"
        else 
            t_isConnected="${FLAG_DISCONNECTED}"
        fi
        
        ## This is kind of unreliable and depends on the order
        if [ "${t_status/100}" != "${t_status}" ]; then
            t_speed="${FLAG_SPEED_100}"
        elif [ "${t_status/10}" != "${t_status}" ]; then
            t_speed="${FLAG_SPEED_10}"
        fi
        
        if [ "${t_status/-FD}" != "${t_status}" ] || [ "${t_status/full duplex}" != "${t_status}" ]; then
            t_duplex="${FLAG_DUPLEX_FULL}"
        elif [ "${t_status/-HD}" != "${t_status}" ] || [ "${t_status/half duplex}" != "${t_status}" ]; then
            t_duplex="${FLAG_DUPLEX_HALF}"
        fi
    fi
    
    echo "${t_intf}: ${t_isConnected} ${t_speed} ${t_duplex}"
}

## Iterate through each interface and retrieve the requested information
for t_intf in ${interfaceList} ; do getInterfaceStatus ${t_intf};  done

## ignore any errors
true
