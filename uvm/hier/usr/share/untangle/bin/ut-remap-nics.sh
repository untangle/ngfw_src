#!/bin/bash
#
# Build new udev file for mapping.
#
UDEV_PERSISTENT_NET_RULES_FILE='/etc/udev/rules.d/70-persistent-net.rules'
UDEV_PERSISTENT_NET_RULES_RULE='SUBSYSTEM=="net", ACTION=="add", DRIVERS=="?*", ATTR{address}=="__MAC_ADDRESS__", ATTR{dev_id}=="0x0", ATTR{type}=="1", KERNEL=="eth*", NAME="__DEVICE_ID__"'

FROM_DEVICES=($1)
TO_DEVICES=($2)
DESTINATION_FILE=$3

if [[ -z $DESTINATION_FILE ]]; then
    DESTINATION_FILE=$UDEV_PERSISTENT_NET_RULES_FILE
fi

if [ ${#FROM_DEVICES[@]} -ne ${#TO_DEVICES[@]} ]; then
    echo "From and To device count do not match."
    exit 1
fi

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
    echo "From and To devices do not contain matching devices"
    echo $DIFF_DEVICES
    exit
fi

OLDIDS=$IFS
IFS=$'\n'

declare -A FROM_MAC_MAP
DEVICE=
for line in $(ip addr); do
    # echo "[$line]"
    # if [[ "$line" =~ ^"[0-9]+: ([^:]):"  ]]; then
    if [[ "$DEVICE" == "" && "$line" =~ ^[0-9]+:" "([^:]+): ]]; then
        DEVICE=${BASH_REMATCH[1]}
        # echo "DEVICE=$DEVICE"
        if [[ $DEVICE == *.*  ]]; then
            DEVICE=
            continue
        fi
    elif [[ "$DEVICE" != "" && "$line" =~ ([0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}) ]]; then
        # echo "MAC=${BASH_REMATCH[1]}"
        FROM_MAC_MAP[$DEVICE]="${BASH_REMATCH[1]}"
        DEVICE=
    fi
done
IFS=$OLDIFS

rm -f $DESTINATION_FILE
touch $DESTINATION_FILE
for i in "${!TO_DEVICES[@]}"; do
    to_device=${TO_DEVICES[i]}
    from_device=${FROM_DEVICES[i]}
    new_rule=$UDEV_PERSISTENT_NET_RULES_RULE
    new_rule="${new_rule/__MAC_ADDRESS__/${FROM_MAC_MAP[$from_device]}/}"
    new_rule="${new_rule/__DEVICE_ID__/$to_device/}"
    echo $new_rule >> $DESTINATION_FILE
done
