#!/bin/bash
##
## Run runtests from NFGW
##
TARGET=local
CLIENT_TARGET=
PORT=22
RUNTESTS_ARGUMENTS=
RUNTESTS_GET_ARGUMENT=
RUNTESTS_VSCODE_DIRECTORY=.vscode/runtests
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

    port=$PORT
    target_address=$target_address
    if [[ $target_address == *":"* ]] ; then
        port=${target_address#*:}
        target_address=${target_address%:*}
    fi

    ssh-copy-id -p $port root@$target_address

    if [ -d $RUNTESTS_VSCODE_DIRECTORY ]; then
        rsync -a -e "ssh -p $port" $RUNTESTS_VSCODE_DIRECTORY root@$target_address:/root
    fi

    TARGET_RESULT=0
    if [ "${CLIENT_TARGET}" != "" ] ; then
        ##
        ## Setup testshell
        ##
        scp $LOCAL_TESTS_TARGET_SCRIPT root@$target_address:$TARGET_TESTS_TARGET_SCRIPT
        ssh root@$target_address "$TARGET_TESTS_TARGET_SCRIPT $CLIENT_TARGET"
        TARGET_RESULT=$?
    fi

    if [ $TARGET_RESULT -ne 0 ] ; then
        echo
        echo "*** Unable to setup target client"
        echo
        continue
    fi

    if [ "${RUNTESTS_ARGUMENTS}" = "setup" ]; then
        # Just perform setup on target.  Useful for configuraring WAN client that needs to connect back.
        continue
    fi

    ##
    ## Build runtests command line
    ##
    # TARGET_COMMAND="/usr/bin/runtests ${RUNTESTS_ARGUMENTS}"
    TARGET_COMMAND="/usr/bin/runtests"
    if [ -f $RUNTESTS_VSCODE_DIRECTORY/overrides.json ] ; then 
        TARGET_COMMAND="$TARGET_COMMAND -o /root/runtests/overrides.json"
    fi
    TARGET_COMMAND="$TARGET_COMMAND ${RUNTESTS_ARGUMENTS}"

    if [ "${RUNTESTS_GET_ARGUMENT}" != "" ] ; then
        ##
        ## Pass get argument
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

    ssh -p $port root@$target_address "$TARGET_COMMAND"
    echo
    ssh -p $port root@$target_address "grep "^Skipped" /tmp/unittest.log"
done
