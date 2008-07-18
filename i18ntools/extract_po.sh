#!/bin/sh

if [ $# -ne 1 ]; then
     echo 1>&2 Usage: $0 "lang_code"
     exit 127
fi

if [ ! -d $1 ]
then
    mkdir $1
fi    

for module in main administration system systemInfo
do 
    cp ../uvm-lib/servlets/webui/po/$1/ung_${module}.po ./$1/
done

for module in webfilter phish spyware spamassassin shield protofilter
do 
    cp ../${module}/po/$1/ung_${module}.po ./$1/
done

cp ../mail-casing/servlets/quarantine/po/$1/ung_mail_casing.po ./$1/
cp ../virus-base/po/$1/ung_virus.po ./$1/

rm -f $1.zip
zip -r $1.zip ./$1/
rm -rf ./$1/

# All done, exit ok
exit 0