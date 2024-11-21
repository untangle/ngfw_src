#! /bin/bash

## Print the status of a list of interfaces.  Each interface should be
## given on the command line.

## Return syntax:
## <interface name>: <connected|disconnected|unknown> <10|100|1000|unknown> <full-duplex|half-duplex|unknown>

FLAG_CONNECTED=CONNECTED
FLAG_DISCONNECTED=DISCONNECTED
FLAG_UNKNOWN=UNKNOWN
FLAG_MISSING=MISSING

FLAG_SPEED_1000=1000
FLAG_SPEED_100=100
FLAG_SPEED_10=10

FLAG_DUPLEX_FULL=FULL_DUPLEX
FLAG_DUPLEX_HALF=HALF_DUPLEX

FLAG_EEE_ENABLED=ENABLED
FLAG_EEE_DISABLED=DISABLED
FLAG_EEE_ACTIVE=ACTIVE
FLAG_EEE_INACTIVE=INACTIVE

interfaceList=$*

function getInterfaceStatus()
{
    local t_intf=$1
    local t_comma=$2
    local t_isConnected=${FLAG_UNKNOWN}
    local t_speed="0"
    local t_duplex=${FLAG_UNKNOWN}
    local t_eee_enabled=${FLAG_UNKNOWN}
    local t_eee_active=${FLAG_UNKNOWN}
    local t_mtu=1500
    local t_link_supported=${FLAG_UNKNOWN}

    if [ -f /sys/class/net/${t_intf}/mtu ]; then
        t_mtu=$(cat /sys/class/net/${t_intf}/mtu)
    fi

    if [ ! -z "${t_comma}" ] ; then 
        echo "    ${t_comma}" ; 
    fi

    if [ ! -e /sys/class/net/${t_intf} ] ; then
        t_isConnected="${FLAG_MISSING}"
    else
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
            local t_status=`mii-tool ${t_intf} 2> /dev/null`
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

        if [ "${t_isConnected}" = "${FLAG_DISCONNECTED}" ] ; then
            # Try to access carrier directly
            if [ -f /sys/class/net/${t_intf}/carrier ] ; then
                if [ "$(cat /sys/class/net/${t_intf}/carrier 2>/dev/null)" = "1" ] ; then
                    t_isConnected="${FLAG_CONNECTED}"
                else
                    t_isConnected="${FLAG_DISCONNECTED}"
                fi
            fi
        fi

        t_status=`ethtool --show-eee ${t_intf} 2>/dev/null | egrep '(EEE status)' | sed -e '{:start;N;s/\n/ /g;t start}'`
        # echo "t_status=$t_status"
        if [ -n "${t_status}" ]; then
            ## Check for link
            if [ "${t_status/EEE status: disabled}" != "${t_status}" ]; then
                # t_eee_enabled="${FLAG_EEE_DISABLED}"
                t_eee_enabled=false
            else
                # t_eee_enabled="${FLAG_EEE_ENABLED}"
                t_eee_enabled=true
                # see if connected
                if [ "${t_status/EEE status: enabled - active}" != "${t_status}" ]; then
                    t_eee_active=true
                fi
            fi
        fi

        # From ethtool, parse:
        #         Supported link modes:   10baseT/Half 10baseT/Full
        #                        100baseT/Half 100baseT/Full
        #                        1000baseT/Full
        # into:
        # M10_HALF_DUPLEX,M10_FULL_DUPLEX,M100_HALF_DUPLEX,M100_FULL_DUPLEX,M1000_FULL_DUPLEX
        t_link_supported=$(\
            ethtool ${t_intf} | \
            sed -n "/Supported link modes/,/Supported pause frame use/p" | \
            sed '$d' | sed "s/Supported link modes://" | \
            sed -z 's/\n//g' | \
            sed -e 's/[\t ]\+/,/g' | \
            sed 's/^,//' |\
            sed -s 's/baseT//g' | \
            sed -s 's#/Full#_FULL_DUPLEX#g' | \
            sed -s 's#/Half#_HALF_DUPLEX#g' \
            )
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
        VENDOR_ID="`awk '/(PCI_ID|PRODUCT)/ { sub( /^[^=]*=/, "" ); sub( /:/, "" ); print tolower($0) }' /sys/class/net/${t_intf}/device/uevent | cut -d/ -f1`"
        VENDOR_ID_SHORT="`awk '/(PCI_ID|PRODUCT)/ { sub( /^[^=]*=/, "" ); sub( /:.*/, "" ); print tolower($0) }' /sys/class/net/${t_intf}/device/uevent | cut -d/ -f1`"

        if [ ! -z "$VENDOR_ID" ] || [ ! -z "$VENDOR_ID_SHORT" ] ; then
            # read from vendor definition file (included in package pciutils)
            VENDOR=""
            if [ -f /usr/share/misc/$BUS.ids ] ; then
                VENDOR="`awk \"/^${VENDOR_ID}/ { \\\$1 = \\\"\\\" ; print \\\$0 }\" /usr/share/misc/$BUS.ids`"
                if [ -z "$VENDOR" ] ; then
                    VENDOR="`awk \"/^${VENDOR_ID_SHORT}/ { \\\$1 = \\\"\\\" ; print \\\$0 }\" /usr/share/misc/$BUS.ids`"
                fi
            fi

            # strip whitespace
            VENDOR="`echo $VENDOR | sed -e 's/^ *//g;s/ *$//g'`"
        fi
    fi
    if [ -f /sys/class/net/${t_intf}/address ] ; then
        MAC_ADDR="`cat /sys/class/net/${t_intf}/address`"
    fi
    
    echo "    {"
    echo "        \"javaClass\" : \"com.untangle.uvm.network.DeviceStatus\","
    echo "        \"deviceName\" :\"${t_intf}\","
    echo "        \"connected\" : \"${t_isConnected}\","
    echo "        \"mbit\" : ${t_speed},"
    echo "        \"duplex\" : \"${t_duplex}\","
    echo "        \"eeeEnabled\" : \"${t_eee_enabled}\","
    echo "        \"eeeActive\" : \"${t_eee_active}\","
    echo "        \"mtu\" : ${t_mtu},"
    echo "        \"vendor\" : \"${VENDOR}\","
    echo "        \"macAddress\" : \"${MAC_ADDR}\"",
    echo "        \"supportedLinkModes\" : \"${t_link_supported}\""
    echo "    }"
}

## Iterate through each interface and retrieve the requested information

echo "{"
echo "    \"javaClass\" : \"java.util.LinkedList\","
echo "    \"list\" : ["

for t_intf in ${interfaceList} ; do 
    getInterfaceStatus ${t_intf} ${COMMA};  
    COMMA=","
done

echo "    ]"
echo "}"

## ignore any errors
true
