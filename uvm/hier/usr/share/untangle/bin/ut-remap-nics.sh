#!/bin/bash
#
# Create new mapping files for interfaces
#
SYSTEMD_NETWORK_PATH=/etc/systemd/network
SYSTEMD_NETWORK_FILE_NAME_TEMPLATE="__DESTINATION_PATH__/10-__TO_DEVICE__.link"
read -r -d '' SYSTEMD_NETWORK_TEMPLATE <<'EOT'
[Match]
MACAddress=__FROM_MAC_ADDRESS__

[Link]
NamePolicy=
Name=__TO_DEVICE__

EOT

FROM_DEVICES=($1)
TO_DEVICES=($2)
DESTINATION_PATH=$3

if [[ -z $DESTINATION_PATH ]]; then
    #
    # Use default path
    #
    DESTINATION_PATH=$SYSTEMD_NETWORK_PATH
fi

if [ ${#FROM_DEVICES[@]} -ne ${#TO_DEVICES[@]} ]; then
    #
    # From/to counts don't match.
    #
    echo "From and To device count do not match."
    exit 1
fi

#
# Compare devices in each list looking for
# any not in the other.
#
DIFF_DEVICES=()
for from_device in "${FROM_DEVICES[@]}"; do
    skip=
    for to_device in "${TO_DEVICES[@]}"; do
        if [[ $from_device == $to_device ]]; then
            skip=1
            break;
        fi
    done
    if [[ -z $skip ]]; then
        DIFF_DEVICES+=($from_device)
    fi
done

if [[ ${#DIFF_DEVICES[@]} -ne 0 ]]; then
    #
    # There is at least one device from either list not in the other.
    #
    echo "From and To devices do not contain matching devices"
    echo $DIFF_DEVICES
    exit 1
fi

#
# Walk list
#
for i in "${!TO_DEVICES[@]}"; do
    from_device=${FROM_DEVICES[i]}
    to_device=${TO_DEVICES[i]}
    to_system_network_file_name=$SYSTEMD_NETWORK_FILE_NAME_TEMPLATE

    # Get from MAC address
    from_mac_address=$(cat /sys/class/net/$from_device/address)

    # Create file name
    if [[ "$to_system_network_file_name" =~ "__DESTINATION_PATH__" ]]; then
        to_system_network_file_name=${to_system_network_file_name//$BASH_REMATCH/$DESTINATION_PATH}
    fi
    if [[ "$to_system_network_file_name" =~ "__TO_DEVICE__" ]]; then
        to_system_network_file_name=${to_system_network_file_name//$BASH_REMATCH/$to_device}
    fi

    #
    # Build up map file from template
    #
    to_system_network=$SYSTEMD_NETWORK_TEMPLATE
    if [[ "$to_system_network" =~ "__FROM_MAC_ADDRESS__" ]]; then
        #
        # Add From MAC address.
        #
        to_system_network=${to_system_network//$BASH_REMATCH/$from_mac_address}
    fi
    if [[ "$to_system_network" =~ "__TO_DEVICE__" ]]; then
        #
        # Specify To device name
        #
        to_system_network=${to_system_network//$BASH_REMATCH/$to_device}
    fi

    #
    # Status
    #
    echo "map $from_mac_address to $to_device"

    #
    # Create file
    #
    echo -e "${to_system_network}" > ${to_system_network_file_name}

done

if [ "$DESTINATION_PATH" == "$SYSTEMD_NETWORK_PATH" ] ; then
    #
    # Installing under systemd, reload daemon
    #
    systemctl daemon-reload
fi

exit 0
