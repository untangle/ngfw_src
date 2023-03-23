#!/bin/bash
##
## Perform explcit renaming of NICs using "ip link set" if udev/systemd fails.
##
## See switch_nic for the algorithm used.
##
## Later (bullseye-era) versions of systemd no longer allow renaming
## of NICs to existing names (e.g.,eth0->eth1, eth1->eth0) as worked in previous releases.
##
## If current mapping matches target, no action is performed.
##

# Original mapping.  Required for lookups.
declare -A ORIGINAL_MAC_NAMES=()
# Active mapping derived from orginal and updated by switching operation.
declare -A CURRENT_MAC_NAMES=()
# Desired mapping from udev or systemd
declare -A TARGET_MAC_NAMES=()

# udev mapping file
UDEV_RULES_FILE_NAME=/etc/udev/rules.d/70-persistent-net.rules
# systemd path for *.link files which define mapping the systemd way
SYSTEMD_NETWORK_PATH=/etc/systemd/network
# Rename prefix
RENAME_PREFIX=rename-

# Debug mode for messages and other output
DEBUG=false
# If true, run in interative mode, otherwise in dameon mode.
INTERACTIVE=false
# If non empty, log debug messages that match substring.  Commonly used to watch substring.
LOG_WATCH=
# If true, only test, don't actually perform rename operations
TEST=false
##
## Process command line arguments
##
while getopts "d:i:l:t:v:" flag; do
    case "${flag}" in
        d) DEBUG=${OPTARG} ;;
        i) INTERACTIVE=${OPTARG} ;;
        l) LOG_WATCH=${OPTARG} ;;
        t) TEST=${OPTARG} ;;
        v) eval "${OPTARG}" ;;
    esac
done
shift $((OPTIND-1))

##
## Log message
##

# Always log messages of this priority
LOG_MESSAGE_PRIORITY_ANY=0
# Only log messages if DEBUG=true
LOG_MESSAGE_PRIORITY_DEBUG=1
if [ "$INTERACTIVE" = "true" ] ; then
	# In interactive mode use echo
	LOGGER_COMMAND="echo"
	LOGGER_TIMESTAMP=date
else
	LOGGER_COMMAND="logger -t interface-mapping"
	LOGGER_TIMESTAMP=
fi

# log_message
# Log a message to specified LOGGER_COMMAND
#
# @param $1    Priority (see above)
# @param $2    Message
function log_message
{
	local priority=$1
	local message=$2

	if [ $priority = $LOG_MESSAGE_PRIORITY_DEBUG ] ; then
		if [ "$DEBUG" = true ]; then
			# Only log if DEBUG is enabled
			$LOGGER_COMMAND $(eval $LOGGER_TIMESTAMP) "debug  $message"
		fi
		if [ "$LOG_WATCH" != "" ] && [ -z "${message##*$LOG_WATCH*}" ]; then
			# Message matches logwatchOnly log if DEBUG is enabled
			$LOGGER_COMMAND $(eval $LOGGER_TIMESTAMP) "debug  $message"
		fi
	elif [ $priority = $LOG_MESSAGE_PRIORITY_ANY ]; then
		# Always log these messages
		if [ "$DEBUG" = true ] || [ "$LOG_WATCH" != "" ] ; then
			# In interactive mode, provide space separation from debug prefix.
			$LOGGER_COMMAND $(eval $LOGGER_TIMESTAMP) "any    $message"
		else
			$LOGGER_COMMAND $(eval $LOGGER_TIMESTAMP) "$message"
		fi
	fi
}

##
## Extract all valid mac addresses and names for all nics on the system
##
function build_current_mac_names
{
	local __function_name="build_current_mac_names"

	while read -r line; do
		info=(${line//;/ })
		name=${info[0]}
		mac_address=${info[1]}
		if [ "$mac_address" = "00:00:00:00:00:00" ] ; then
			# No mapping for this kind of mac
			log_message $LOG_MESSAGE_PRIORITY_DEBUG "$__function_name: ignore $mac_address, $name"
			continue
		fi
		log_message $LOG_MESSAGE_PRIORITY_DEBUG "$__function_name: add $mac_address, $name to current and original"
		CURRENT_MAC_NAMES[$mac_address]=$name
		ORIGINAL_MAC_NAMES[$mac_address]=$name
	done < <(find /sys/class/net -mindepth 1 -maxdepth 1 -name eth* -printf "%P;" -execdir cat {}/address \;)

}

##
## Build mac address to name mapping from udev rules.
## Format of this file is:
## SUBSYSTEM=="net", ACTION=="add", DRIVERS=="?*", ATTR{address}=="40:62:31:01:1c:91", ATTR{dev_id}=="0x0", ATTR{type}=="1", KERNEL=="eth*", NAME="eth3"
##
function build_udev_map
{
	local __function_name="build_udev_map"

	log_message $LOG_MESSAGE_PRIORITY_DEBUG "$__function_name: reading from $UDEV_RULES_FILE_NAME"
	while read -r line; do
		mac_address=
		name=
		if [[ $line =~ "ATTR{address}==\""([^\"]+)"\"" ]]; then
			mac_address=${BASH_REMATCH[1]}
		fi
		if [[ $line =~ "NAME=\""([^\"]+)"\"" ]]; then
			name=${BASH_REMATCH[1]}
		fi
		log_message $LOG_MESSAGE_PRIORITY_DEBUG "$__function_name: found mac_address=$mac_address, name=$name"
		if [ "$mac_address" != "" ] && [ "$name" != "" ] ; then
			log_message $LOG_MESSAGE_PRIORITY_ANY "$__function_name: add $mac_address, $name to target"
			TARGET_MAC_NAMES[$mac_address]=$name
		fi
	done < $UDEV_RULES_FILE_NAME
}

##
## Build mac address to name mapping from systemd/network link files
## Format of this file is:
## [Match]
## MACAddress=40:62:31:01:1c:92
## ...
##
## [Link]
## Name=eth1
##
function build_systemd_map
{
	local __function_name="build_systemd_map"

	if [ "$(ls -1 $SYSTEMD_NETWORK_PATH/*.link 2>/dev/null)" = "" ]; then
		# No files exist
		log_message $LOG_MESSAGE_PRIORITY_DEBUG "$__function_name: no .link files found"
		return
	fi

	for link_file_name in $SYSTEMD_NETWORK_PATH/*.link; do
		mac_address=
		name=
		while read -r line; do
			if [[ $line =~ ^MACAddress=(.+) ]]; then
				mac_address=${BASH_REMATCH[1]}
			fi
			if [[ $line =~ ^Name=(.+) ]]; then
				name=${BASH_REMATCH[1]}
			fi
		done < $link_file_name
		log_message $LOG_MESSAGE_PRIORITY_DEBUG "$__function_name: found $link_file_name, mac_address=$mac_address, name=$name"
		if [ "$mac_address" != "" ] && [ "$name" != "" ] ; then
			log_message $LOG_MESSAGE_PRIORITY_ANY "$__function_name: $link_file_name, add $mac_address, $name to target"
			TARGET_MAC_NAMES[$mac_address]=$name
		fi
	done

}

##
## Get mac address using name from original mapping
##
function get_original_mac_address_from_name
{
	local __function_name="get_original_mac_address_from_name"
	name=$1

	matching_mac_address=
	for mac_address in "${!ORIGINAL_MAC_NAMES[@]}"; do
		if [ ${ORIGINAL_MAC_NAMES[$mac_address]} = "$name" ] ; then
			matching_mac_address=$mac_address
			break
		fi
	done

	echo $matching_mac_address
}

##
## Get mac address using name from current mapping.
##
function get_current_mac_address_from_name
{
	local __function_name="get_current_mac_address_from_name"
	name=$1

	matching_mac_address=
	for mac_address in "${!CURRENT_MAC_NAMES[@]}"; do
		if [ ${CURRENT_MAC_NAMES[$mac_address]} = "$name" ] ; then
			matching_mac_address=$mac_address
			break
		fi
	done

	echo $matching_mac_address
}

##
## Perform rename of nic using ip link commands
##
function rename_nic
{
	local __function_name="rename_nic"
	local source_name=$1
	local destination_name=$2

	if [ "$TEST" = "false" ] ; then
		log_message $LOG_MESSAGE_PRIORITY_ANY "$__function_name: rename $source_name to $destination_name"
		ip link set $source_name down
		ip link set $source_name name $destination_name
		ip link set $destination_name up
	else
		log_message $LOG_MESSAGE_PRIORITY_ANY "$__function_name: (test=true) not rename $source_name to $destination_name"
	fi
}

##
## Switch nic with a current and target name
##
## Algorithm example:
## Consider:
## Current mapping of:
##	40:62:31:01:1c:92	eth2
##	40:62:31:01:1c:93	eth3
##	40:62:31:01:1c:91	eth1
## Target mapping of:
##	40:62:31:01:1c:91	eth3
##	40:62:31:01:1c:92	eth1
##	40:62:31:01:1c:93	eth2
##
## Processing would go as follows:
## switch_nic: eth3 (40:62:31:01:1c:93) -> eth2 (40:62:31:01:1c:92)
##	rename_nic: rename eth2 to rename-eth2
##	rename_nic: rename eth3 to eth2
## switch_nic: rename-eth2 (40:62:31:01:1c:92) -> eth1 (40:62:31:01:1c:91)
##	rename_nic: rename eth1 to rename-eth1
##	rename_nic: rename rename-eth2 to eth1
## switch_nic: rename-eth1 (40:62:31:01:1c:91) -> eth3 (40:62:31:01:1c:93)
##	IMPORTANT: The current mapping of eth3 is now eth2
##	rename_nic: rename rename-eth1 to eth3
##
function switch_nic
{
	local __function_name="switch_nic"
	local current_name=$1
	local target_name=$2

	current_mac_address=$(get_current_mac_address_from_name $current_name)
	# IMPORTANT: Target must come from original mapping, not current!
        target_mac_address=$(get_original_mac_address_from_name $target_name)
	log_message $LOG_MESSAGE_PRIORITY_DEBUG "$__function_name: current=$current_name ($current_mac_address) -> target=$target_name ($target_mac_address)"

	if [ ${CURRENT_MAC_NAMES[$target_mac_address]} = "$target_name" ]; then
		# Only rename current if it's the same as the target name
		log_message $LOG_MESSAGE_PRIORITY_DEBUG "$__function_name: map, current name=target name ($target_name)"
		old_name=$target_name
		new_name=$RENAME_PREFIX$target_name
		CURRENT_MAC_NAMES[$target_mac_address]=$RENAME_PREFIX$target_name
		rename_nic $old_name $new_name
	else
		log_message $LOG_MESSAGE_PRIORITY_DEBUG "$__function_name: no map, current name != target (${CURRENT_MAC_NAMES[$target_mac_address]} vs. $target_name)"
	fi

	log_message $LOG_MESSAGE_PRIORITY_DEBUG "$__function_name: map current mac to target name"
	old_name=$current_name
	new_name=$target_name
	CURRENT_MAC_NAMES[$current_mac_address]=$target_name
	rename_nic $old_name $new_name

}

##
## Main
##

##
## Build original and current mappings
##
build_current_mac_names

##
## Read target mappings
##
if [ -f $UDEV_RULES_FILE_NAME ]; then
	build_udev_map
else
	build_systemd_map
fi

if [ ${#TARGET_MAC_NAMES[@]} -eq 0 ]; then
	## No target mapping found
	log_message $LOG_MESSAGE_PRIORITY_DEBUG "no target mapping found"
	exit
fi

##
## Walk target mapping and perform switches against current mapping
##
for mac_address in "${!TARGET_MAC_NAMES[@]}"; do
	target_name=${TARGET_MAC_NAMES[$mac_address]}
	current_name=${CURRENT_MAC_NAMES[$mac_address]}
	if [ "$current_name" = "$target_name" ]; then
   		continue
	fi
	if [ "$current_name" = "" ]; then
		log_message $LOG_MESSAGE_PRIORITY_ANY "final mapping: target wants to map to mac_address=$mac_address, but not found on current system"
		continue
	fi
	log_message $LOG_MESSAGE_PRIORITY_DEBUG "final mapping: switch current_name=$current_name to target_name=$target_name"
	switch_nic $current_name $target_name
done

##
## Sumary of final mapping
##
for mac_address in "${!CURRENT_MAC_NAMES[@]}"; do
	current_name=${CURRENT_MAC_NAMES[$mac_address]}
	log_message $LOG_MESSAGE_PRIORITY_ANY "final mapping: $mac_address->$current_name"
done
