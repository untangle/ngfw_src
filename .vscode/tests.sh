#!/bin/bash
##
## Run runtests from NFGW
##
TARGET=local
CLIENT_TARGET=
PORT=22
RUNTESTS_ARGUMENTS=
RUNTESTS_GET_ARGUMENT=

##
## Read command-line arguments
##
while getopts "t:h:p:gr:" flag; do
    case "${flag}" in
        r) RUNTESTS_ARGUMENTS=${OPTARG};;
        t) TARGET=${OPTARG} ;;
        h) CLIENT_TARGET=${OPTARG} ;;
        p) PORT=${OPTARG} ;;
        g) RUNTESTS_GET_ARGUMENT=-g ;;
        *) echo "Unknown argument ${!OPTIND}"
        ;;
    esac
done
shift $((OPTIND-1))

echo "TARGET=$TARGET"
echo "CLIENT_TARGET=$CLIENT_TARGET"
echo "RUNTESTS_ARGUMENTS=$RUNTESTS_ARGUMENTS"
echo "RUNTESTS_GET_ARGUMENT=$RUNTESTS_GET_ARGUMENT"

# Break target down by commas into an array.
TARGET_ADDRESSES=()
while IFS=',' read -ra ADDRESSES; do
    for address in "${ADDRESSES[@]}"; do
        TARGET_ADDRESSES+=($address)
    done
done <<< "$TARGET"

LOCAL_TESTS_TARGET_SCRIPT=./.vscode/tests_target.sh
TARGET_TESTS_TARGET_SCRIPT=/root/tests_target.sh
CLIENT_PUBLIC_KEY=/usr/lib/runtests/test_shell.pub
CLIENT_SSH_OPTIONS="-o StrictHostKeyChecking=no -o ConnectTimeout=300 -o ConnectionAttempts=15 -i $CLIENT_PRIVATE_KEY"

##
## Run tests from each NGFW.
##
for target_address in "${TARGET_ADDRESSES[@]}"; do
    echo "Running tests on $target_address..."

    ssh-copy-id -p $PORT root@$target_address

    if [ "${CLIENT_TARGET}" != "" ] ; then
        ##
        ## Setup testshell
        ##
        scp $LOCAL_TESTS_TARGET_SCRIPT root@$target_address:$TARGET_TESTS_TARGET_SCRIPT
        ssh root@$target_address "$TARGET_TESTS_TARGET_SCRIPT $CLIENT_TARGET"
    fi

    ##
    ## Build runtests command line
    ##
    TARGET_COMMAND="/usr/bin/runtests ${RUNTESTS_ARGUMENTS}"
    if [ "${RUNTESTS_GET_ARGUMENT}" != "" ] ; then
        ##
        ## Pass get argumnt
        ##
        TARGET_COMMAND="$TARGET_COMMAND ${RUNTESTS_GET_ARGUMENT}"
    fi
    if [ "$CLIENT_TARGET" != "" ]; then
        ##
        ## External LAN client specified
        ##
        TARGET_COMMAND="$TARGET_COMMAND -h $CLIENT_TARGET"
    fi
    echo
    echo "$TARGET_COMMAND"
    echo
    ssh root@$target_address "$TARGET_COMMAND"
    echo
    ssh root@$target_address "grep "^Skipped" /tmp/unittest.log"
done
