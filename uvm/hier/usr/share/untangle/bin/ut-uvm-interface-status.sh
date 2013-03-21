#! /bin/bash

## Print the status of a list of interfaces.  Each interface should be
## given on the command line.

## Return syntax:
## <interface name>: <connected|disconnected|unknown> <10|100|1000|unknown> <full-duplex|half-duplex|unknown>

FLAG_CONNECTED=CONNECTED
FLAG_DISCONNECTED=DISCONNECTED
FLAG_UNKNOWN=UNKNOWN

FLAG_SPEED_1000=1000
FLAG_SPEED_100=100
FLAG_SPEED_10=10

FLAG_DUPLEX_FULL=FULL_DUPLEX
FLAG_DUPLEX_HALF=HALF_DUPLEX

interfaceList=$*

function getInterfaceStatus()
{
    local t_intf=$1
    local t_isConnected=${FLAG_UNKNOWN}
    local t_speed="0"
    local t_duplex=${FLAG_UNKNOWN}

    t_status=`ethtool ${t_intf} | grep -v Supported | egrep '(Speed|Duplex|Link detected)' | sed -e '{:start;N;s/\n/ /g;t start}'`
    if [ -n "${t_status}" ]; then
        ## Check for link
        if [ "${t_status/Link detected: yes}" != "${t_status}" ]; then
            t_isConnected="${FLAG_CONNECTED}"
        
            if [ "${t_status/Speed: }" != "${t_status}" ]; then
                t_speed=`echo ${t_status} | cut -d " " -f 2 | sed 's/[^0-9]//g'`
                if [ -z "$t_speed" ] ; then t_speed="0" ; fi
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
        if [ "${t_status/1000}" != "${t_status}" ]; then
            t_speed="${FLAG_SPEED_1000}"
        elif [ "${t_status/100}" != "${t_status}" ]; then
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

    export VENDOR=""
    BUS=""
    ls -l /sys/class/net/${t_intf}/subsystem/${t_intf} 2>/dev/null | grep -q 'devices/pci'
    if [[ $? -eq 0 ]] ; then
        BUS="pci"
    fi
    ls -l /sys/class/net/${t_intf}/subsystem/${t_intf} 2>/dev/null | grep -q 'devices/usb'
    if [[ $? -eq 0 ]] ; then
        BUS="usb"
    fi
    if [ -f /sys/class/net/${t_intf}/device/uevent ] && [ ! -z "$BUS" ] ; then
        # find vendor ID
        VENDOR_ID="`cat /sys/class/net/${t_intf}/device/uevent | awk '/(PCI_ID|PRODUCT)/ { sub( /^[^=]*=/, "" ); print tolower($0) }' | sed -e 's/:/ /'`"
        VENDOR_ID_SHORT="`cat /sys/class/net/${t_intf}/device/uevent | awk '/(PCI_ID|PRODUCT)/ { sub( /^[^=]*=/, "" ); sub( /:.*/, "" ); print tolower($0) }'`"

        # read from vendor definition file
        VENDOR="`awk \"/^${VENDOR_ID}/ { \\\$1 = \\\"\\\" ; print \\\$0 }\" /usr/share/misc/$BUS.ids`"
        if [ -z "$VENDOR" ] ; then
            VENDOR="`awk \"/^${VENDOR_ID_SHORT}/ { \\\$1 = \\\"\\\" ; print \\\$0 }\" /usr/share/misc/$BUS.ids`"
        fi

        # strip whitespace
        VENDOR="`echo $VENDOR | sed -e 's/^ *//g;s/ *$//g'`"
    fi
    
    echo "    {"
    echo "        \"javaClass\" : \"com.untangle.uvm.network.DeviceStatus\","
    echo "        \"deviceName\" :\"${t_intf}\","
    echo "        \"connected\" : \"${t_isConnected}\","
    echo "        \"mbit\" : ${t_speed},"
    echo "        \"duplex\" : \"${t_duplex}\","
    echo "        \"vendor\" : \"${VENDOR}\""
    echo "    }"
}

## Iterate through each interface and retrieve the requested information

echo "{"
echo "    \"javaClass\" : \"java.util.LinkedList\","
echo "    \"list\" : ["

for t_intf in ${interfaceList} ; do 
    if [ ! -z "$COMMA" ] ; then echo "    $COMMA" ; fi
    getInterfaceStatus ${t_intf};  
    COMMA=","
done

echo "    ]"
echo "}"

## ignore any errors
true
