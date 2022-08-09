#! /bin/bash
# Perform SNMP related actions (that should eventually migrate to sync-settings)

SNMP_CONFIG=/usr/bin/net-snmp-config

SNMP_ETC_PATH=/etc/snmp
SNMP_HACK_PATH=/snmp

EXIT_CODE=0

# create_snmp3_user
# Create an snmp3 user.
#
# NOTE: Current version of snmp has a bug that that unconditionally writes
# to /snmp/snmpd.conf withput the ability to override.
# To get around this issue, we bind mount /etc/snmpd to /snmp before
# calling /usr/bin/net-snmp-config then unmount when finished.
#
# @param $2     Username
# @param $3     Auth protocol (md5 or sha)
# @param $4     Auth passphrase
# @param $5     Privacy protocol (des or aes)
# @param $6     Privacy protocol passphrase
create_snmp3_user()
{
    user_name=$2
    auth_protocol=$3
    auth_passphrase=$4
    privacy_protocol=$5
    privacy_passphrase=$6

    mkdir $SNMP_HACK_PATH
    mount --bind $SNMP_ETC_PATH $SNMP_HACK_PATH

    privacy_passphrase_argument=""
    if [ "$privacy_passphrase" != "" ]; then
        privacy_passphrase_argument="-x $privacy_protocol -X \"$privacy_passphrase\""
    fi
    command="$SNMP_CONFIG --create-snmpv3-user -a $auth_protocol -A \"$auth_passphrase\" $privacy_passphrase_argument $user_name"
    eval $command
    EXIT_CODE=$?

    umount $SNMP_HACK_PATH
    rmdir $SNMP_HACK_PATH

}

$1 "$@"

exit $EXIT_CODE