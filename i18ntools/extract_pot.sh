#!/bin/sh

if [ ! -d pot ]
then
    mkdir pot
fi    

for module in main administration system systemInfo upgrade
do 
    cp ../uvm-lib/servlets/webui/po/ung_${module}.pot ./pot/
done

for module in webfilter phish spyware spamassassin shield protofilter ips
do 
    cp ../${module}/po/ung_${module}.pot ./pot/
done

cp ../mail-casing/servlets/quarantine/po/ung_mail_casing.pot ./pot/
cp ../virus-base/po/ung_virus.pot ./pot/

rm -f pot.zip
zip -r pot.zip ./pot/
rm -rf ./pot/

# All done, exit ok
exit 0