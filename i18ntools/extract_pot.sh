#!/bin/sh

if [ ! -d pot ]
then
    mkdir pot
fi    


cp ../uvm-lib/po/untangle-libuvm.pot ./pot/
cp ../../pkgs/untangle-apache2-config/po/untangle-apache2-config.pot ./pot/
cp ../../pkgs/untangle-net-alpaca/po/untangle-net-alpaca.pot ./pot/
cp ../../../internal/isotools/installer-pkgs-additional/untangle-system-stats/debian/po/templates.pot ./pot/untangle-system-stats.pot
# lines below is part of winuntangle which has been EOL.
# cp ../../../internal/isotools/wintangle-systray/untangle-systray.pot ./pot/
# cp ../../../internal/isotools/wintangle-installer/LanguageStrings.nsh ./pot/

for module in ../* ../../../hades/rup/* ; do
    module_suffix=`basename ${module} | sed -e 's|-base||' -e 's|-casing||'`
    module_pot="${module}/po/untangle-*-${module_suffix}.pot"
    [ -f  ${module_pot} ] && {
        echo "cp ${module_pot} pot"
        cp ${module_pot} pot
    }
done

rm -f pot.zip
zip -r pot.zip ./pot/
rm -rf ./pot/

# All done, exit ok
exit 0