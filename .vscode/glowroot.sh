#!/bin/bash
##
## Download latest glowroot package into glowroot directory
## and configure ngfw to use it.
##
GLOWROOT_DIRECTORY=$PWD/.glowroot 
GLOWROOT_JAR=$GLOWROOT_DIRECTORY/glowroot.jar 
UNTANGLE_VM_CONF=$PWD/dist/usr/share/untangle/conf/untangle-vm.conf
NETWORK_JS=$PWD/dist/usr/share/untangle/settings/untangle-vm/network.js

if [ ! -f $GLOWROOT_DIRECTORY/glowroot.jar ] ; then
    ##
    ## Dowwnload and install
    ##
    echo
    echo "Downloading and installing glowroot..."
    echo
    cd $GLOWROOT_DIRECTORY
    curl -s https://api.github.com/repos/glowroot/glowroot/releases/latest  \
        | python -c "import sys, json; print(json.load(sys.stdin)['assets'][0]['browser_download_url'])" \
        | xargs wget
    unzip *.zip
    mv glowroot/* .
    rmdir glowroot
fi

grep -q $GLOWROOT_JAR $UNTANGLE_VM_CONF
if [ $? -eq 1 ] ; then
    ##
    ## Configure ngfw to use
    ##
    echo
    echo "Configuring untangle-vm.conf..."
    echo
    sed -i \
        '/java_opts=""/a java_opts += "-javaagent:'$GLOWROOT_JAR'"' \
        $UNTANGLE_VM_CONF
fi

##
## Determine if access rule is defined.
##
GLOWROOT_ADMIN_PORT=$( \
    cat $GLOWROOT_DIRECTORY/admin.json \
    | python -c "import sys, json; print(json.load(sys.stdin)['web']['port'])"
)
##
## Just a simple grep of access rules for 4000
##
cat $NETWORK_JS \
    | python -c "import sys, json; print(json.load(sys.stdin)['accessRules']['list'])" \
    | grep -q $GLOWROOT_ADMIN_PORT
if [ $? -eq 1 ] ; then
    echo
    echo "There does not appear to be an access port rule defined!"
    echo
fi


echo
echo "Glowroot ready!"
echo 