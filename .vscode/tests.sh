#!/bin/bash
##
## Run runtests from NFGW
##
TARGET=local
CLIENT_TARGET=
PORT=22
TEST_SUITES=
TESTS=

##
## Read command-line arguments
##
while getopts ":t:h:s:p:" flag; do
    case "${flag}" in
        t) TARGET=${OPTARG} ;;
        h) CLIENT_TARGET=${OPTARG} ;;
        s) TEST_SUITES=${OPTARG%% *} TESTS=${OPTARG#* } ;;
        p) PORT=${OPTARG} ;;
    esac
done
shift $((OPTIND-1))

if [ "$TESTS" == "$TEST_SUITES" ] ; then
    ##
    ## If tests = test suites, it means there was no space and only suite shoudl be run.
    ##
    TESTS=
fi

echo "CLIENT_TARGET=$CLIENT_TARGET"
echo "TARGET=$TARGET"
echo "TEST_SUITES=$TEST_SUITES"
echo "TESTS=$TESTS"

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

    ##
    ## Setup SSH between 
    ##
    scp $LOCAL_TESTS_TARGET_SCRIPT root@$target_address:$TARGET_TESTS_TARGET_SCRIPT
    ssh root@$target_address "$TARGET_TESTS_TARGET_SCRIPT $CLIENT_TARGET"

    ##
    ## Build runtests command line
    ##
    TARGET_COMMAND="/usr/bin/runtests -t $TEST_SUITES"
    if [ "$TESTS" != "" ] ; then
        ##
        ## Specific tests within the suite(s)
        ##
        TARGET_COMMAND="$TARGET_COMMAND -T $TESTS"
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
