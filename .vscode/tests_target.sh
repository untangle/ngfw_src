#!/bin/bash
##
## Installer script run from NGW to setup target testshell client.
##
CLIENT_TARGET=$1

##
## Build SSH command base we will use to communicate with client
##
SOURCE_KEY=/usr/lib/runtests/test_shell.key
SSH_COMMAND="ssh -i $SOURCE_KEY -o StrictHostKeyChecking=no -o ConnectTimeout=300 -o ConnectionAttempts=15 root@$CLIENT_TARGET"

echo "SSH_COMAND=$SSH_COMMAND"

CONNECT_VERIFY=$(eval $SSH_COMMAND "pwd")
if [ "$CONNECT_VERIFY" = "" ] ; then
    # Unable to access via key
    echo "Unable to access $CLIENT_TARGET with $SOURCE_KEY"
    exit 1
fi

SETUP_TESTSHELL_SCRIPT=/root/setup_testshell.sh
SETUP_TESTSHELL_URI=http://test.untangle.com/test/setup_testshell.sh

## Install required Debian packages
eval '$SSH_COMMAND "apt-get update && apt-get -y install wget sudo resolvconf"'
## Get testshell script and run
eval '$SSH_COMMAND "cd /root; wget -O $SETUP_TESTSHELL_SCRIPT $SETUP_TESTSHELL_URI "'
eval '$SSH_COMMAND "chmod 775 $SETUP_TESTSHELL_SCRIPT"'
eval '$SSH_COMMAND "echo y | $SETUP_TESTSHELL_SCRIPT"'

## Add testshell to sudoers
grep -q "testshell ALL=(ALL) NOPASSWD: ALL" /etc/sudoers
if [ $? -ne 0 ] ; then
    eval '$SSH_COMMAND "echo \"testshell ALL=(ALL) NOPASSWD: ALL\" >> /etc/sudoers"'
fi

