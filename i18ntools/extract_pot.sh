#!/bin/sh

if [ ! -d pot ]
then
    mkdir pot
fi    


cp ../uvm-lib/po/untangle-libuvm.pot ./pot/
cp ../gui/po/untangle-install-wizard.pot ./pot/
cp ../../pkgs/untangle-apache2-config/po/untangle-apache2-config.pot ./pot/
cp ../mail-casing/po/untangle-casing-mail.pot ./pot/
cp ../virus-base/po/untangle-base-virus.pot ./pot/
cp ../webfilter-base/po/untangle-base-webfilter.pot ./pot/

for module in untangle-node-phish untangle-node-spyware untangle-node-spamassassin untangle-node-shield untangle-node-protofilter untangle-node-ips untangle-node-firewall untangle-node-reporting untangle-node-openvpn
do 
    module_dir=`echo "${module}"|cut -d"-" -f3`
    cp ../${module_dir}/po/${module}.pot ./pot/
done

for module in untangle-node-adconnector untangle-node-boxbackup untangle-node-portal untangle-node-pcremote
do 
    module_dir=`echo "${module}"|cut -d"-" -f3`
    cp ../../../hades/rup/${module_dir}/po/${module}.pot ./pot/
done


rm -f pot.zip
zip -r pot.zip ./pot/
rm -rf ./pot/

# All done, exit ok
exit 0