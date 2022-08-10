#!/bin/bash
DEV_ENVIRONMENT=local
TARGET=
PORT=22
RAKE_LOG=rake.log

while getopts "t:p:e:r:" flag; do
    case "${flag}" in
        t) TARGET=${OPTARG} ;;
        p) PORT=${OPTARG} ;;
        e) DEV_ENVIRONMENT=${OPTARG} ;;
        r) RAKE_LOG=${OPTARG} ;;
    esac
done
shift $((OPTIND-1))

echo "TARGET=$TARGET"
echo "DEV_ENVIRONMENT=$DEV_ENVIRONMENT"
echo "RAKE_LOG=$RAKE_LOG"

if [ "$DEV_ENVIRONMENT" == "remote" ]; then
    ##
    ## Build in docker DEV_ENVIRONMENT
    ##
    docker-compose -f docker-compose.build.yml run pkgtools
    VERBOSE=1 NO_CLEAN=1 RAKE_LOG=$RAKE_LOG DEV_ENVIRONMENT=$DEV_ENVIRONMENT docker-compose -f docker-compose.build.yml up dev-build
elif [ "$DEV_ENVIRONMENT" == "remote"]; then
    ##
    ## Compile ngfw and restart uvm if neccessary.
    ##
    rm -f $RAKE_LOG
    rake |& tee $RAKE_LOG
else
    echo "Unknown DEV_ENVIRONMENT=$DEV_ENVIRONMENT"
    exit
fi

##
## Review output of build
##
if [ $(grep -c "missing documentation" $RAKE_LOG) -gt 0 ] ; then 
    exit
fi

if [ $(grep -c error $RAKE_LOG) -gt 0 ] ; then 
    exit
fi

if [ $(grep -c "jslint failed" $RAKE_LOG) -gt 0 ] ; then 
    exit
fi

##
## If java modiified, restart uvm
##
RESTART=0
if [ -f $RAKE_LOG ] && [ $(grep -c javac $RAKE_LOG) -gt 0 ]; then 
    RESTART=1
    if [ "$DEV_ENVIRONMENT" == "local" ]; then
        ./dist/etc/init.d/untangle-vm restart
    fi
fi

# Break target down by commas into an array.
TARGET_ADDRESSES=()
while IFS=',' read -ra ADDRESSES; do
    for address in "${ADDRESSES[@]}"; do
        TARGET_ADDRESSES+=($address)
    done
done <<< "$TARGET"

for target_address in "${TARGET_ADDRESSES[@]}"; do
    echo "Copying to $target_address..."

    ssh-copy-id -p $PORT root@$target_address
    rsync -a dist/usr/lib root@$address:/usr
    rsync -a dist/usr/share/untangle/lib root@$address:/usr/share/untangle
    rsync -a dist/usr/share/untangle/bin root@$address:/usr/share/untangle
    rsync -a dist/usr/share/untangle/web root@$address:/usr/share/untangle

    if [ $RESTART -eq 1 ] ; then
        ssh root@$address "/etc/init.d/untangle-vm restart"
    fi

done
