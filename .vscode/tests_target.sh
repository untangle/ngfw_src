#!/bin/sh
##
## Installer script run from NGW to setup target testshell client.
##
CLIENT_TARGET=$1

SETUP_TESTSHELL_SCRIPT=/root/setup_testshell.sh
SETUP_TESTSHELL_URI=http://test.untangle.com/test/setup_testshell.sh

##
## Build SSH command base we will use to communicate with client
##
SOURCE_KEY=/usr/lib/runtests/test_shell.key
SOURCE_PUB_KEY=/usr/lib/runtests/test_shell.pub
chmod 600 $SOURCE_KEY
SSH_COMMAND="ssh -i $SOURCE_KEY -o StrictHostKeyChecking=no -o ConnectTimeout=300 -o ConnectionAttempts=15 root@$CLIENT_TARGET"

echo "SSH_COMAND=$SSH_COMMAND"

# This will always fail until we can come up with a way to prompt password from within script.
ssh_copy_id=$(which ssh-copy-id || which /root/ssh-copy-id)
$ssh_copy_id -f -i $SOURCE_PUB_KEY root@$CLIENT_TARGET

CONNECT_VERIFY=$(eval $SSH_COMMAND "pwd")
if [ "$CONNECT_VERIFY" = "" ] ; then
    echo "***"
    echo
    echo "Unable to communicate using private key."
    echo 
    echo "Verify in /etc/sshd_config:"
    echo "  PermitRootLogin yes"
    echo
    echo "Setup key from appliance using:"
    echo "$ssh_copy_id -f -i $SOURCE_PUB_KEY root@$CLIENT_TARGET"
    echo
    echo "***"
    exit 1
fi

## Install required Debian packages
eval '$SSH_COMMAND "apt-get update && apt-get -y install wget sudo resolvconf"'
## Get testshell script and run
eval '$SSH_COMMAND "cd /root; wget -O $SETUP_TESTSHELL_SCRIPT $SETUP_TESTSHELL_URI "'
eval '$SSH_COMMAND "chmod 775 $SETUP_TESTSHELL_SCRIPT"'
eval '$SSH_COMMAND "echo y | $SETUP_TESTSHELL_SCRIPT"'

## !!!! see if resolv needs to be re-written...

## Add testshell to sudoers
## !!! check if it really needs to be added on client
if [ $? -ne 0 ] ; then
    eval '$SSH_COMMAND "echo \"testshell ALL=(ALL) NOPASSWD: ALL\" >> /etc/sudoers"'

fi

