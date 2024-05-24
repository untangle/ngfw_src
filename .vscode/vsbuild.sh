#!/bin/bash
DEV_ENVIRONMENT=local
TARGET=
PORT=22
RAKE_LOG=rake.log
BUILD_TYPE=rake

while getopts "t:p:e:r:b:" flag; do
    case "${flag}" in
        t) TARGET=${OPTARG} ;;
        p) PORT=${OPTARG} ;;
        e) DEV_ENVIRONMENT=${OPTARG} ;;
        r) RAKE_LOG=${OPTARG} ;;
        b) BUILD_TYPE=${OPTARG} ;;
    esac
done
shift $((OPTIND-1))

echo "TARGET=$TARGET"
echo "DEV_ENVIRONMENT=$DEV_ENVIRONMENT"
echo "BUILD_TYPE=$BUILD_TYPE"
echo "RAKE_LOG=$RAKE_LOG"

if [ "$DEV_ENVIRONMENT" == "remote" ]; then
    ##
    ## Build in docker DEV_ENVIRONMENT
    ##
    ## Update pkgtools volume (we don't need to keep container)
    docker-compose -f docker-compose.build.yml run --rm pkgtools
    VERBOSE=1 NO_CLEAN=1 RAKE_LOG=$RAKE_LOG DEV_ENVIRONMENT=$DEV_ENVIRONMENT BUILD_TYPE=$BUILD_TYPE docker-compose -f docker-compose.build.yml up dev-build
elif [ "$DEV_ENVIRONMENT" != "remote" ]; then
    ##
    ## Compile ngfw and restart uvm if neccessary.
    ##
    rm -f $RAKE_LOG
    rake |& tee $RAKE_LOG
else
    echo "Unknown DEV_ENVIRONMENT=$DEV_ENVIRONMENT"
    exit 1
fi

##
## Review output of build
##
if [ $(grep -c "missing documentation" $RAKE_LOG) -gt 0 ] ; then 
    exit 1
fi

if [ $(grep -c "missing uri" $RAKE_LOG) -gt 0 ] ; then 
    exit 1
fi

if [ $(grep -c error $RAKE_LOG) -gt 0 ] ; then 
    exit 1
fi

if [ $(grep -c "jslint failed" $RAKE_LOG) -gt 0 ] ; then 
    exit 1
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
    echo "Synchronizing to $target_address..."

    port=$PORT
    target_address=$target_address
    if [[ $target_address == *":"* ]] ; then
        port=${target_address#*:}
        target_address=${target_address%:*}
    fi

    echo "ssh-copy-id -p $port root@$target_address"
    ssh-copy-id -p $port root@$target_address

    rsync -a -e "ssh -p $port" dist/etc/* root@$target_address:/etc
    rsync -a -e "ssh -p $port" dist/usr/lib root@$target_address:/usr
    rsync -a -e "ssh -p $port" dist/usr/share/java/uvm/* root@$target_address:/usr/share/java/uvm
    rsync -a -e "ssh -p $port" dist/usr/share/untangle/bin root@$target_address:/usr/share/untangle
    rsync -a -e "ssh -p $port" dist/usr/share/untangle/lib root@$target_address:/usr/share/untangle
    rsync -a -e "ssh -p $port" dist/usr/share/untangle/conf/log4j.xml root@$target_address:/usr/share/untangle/conf/log4j.xml
    rsync -a -e "ssh -p $port" dist/usr/share/untangle/conf/untangle-vm.conf root@$target_address:/usr/share/untangle/conf/untangle-vm.conf
    rsync -a -e "ssh -p $port" dist/usr/share/untangle/web root@$target_address:/usr/share/untangle
    rsync -a -e "ssh -p $port" dist/usr/share/untangle/lang/lang.cfg root@$target_address:/usr/share/untangle/lang
    rsync -a -e "ssh -p $port" dist/usr/share/untangle/conf root@$target_address:/usr/share/untangle

    ssh -p $port root@$target_address "systemctl daemon-reload"

    if [ $RESTART -eq 1 ] ; then
        ssh -p $port root@$target_address "/etc/init.d/untangle-vm restart"
    fi
    echo "...completed"

done
