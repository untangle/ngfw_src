#!/bin/sh

if [ ! -d pot ]
then
    mkdir pot
fi    


cp ../uvm/po/untangle-libuvm.pot ./pot/
cp ../../pkgs/untangle-apache2-config/po/untangle-apache2-config.pot ./pot/
cp ../../../internal/isotools/installer-pkgs-additional/untangle-system-stats/debian/po/templates.pot ./pot/untangle-system-stats.pot

for module in ../* ../../../hades/src/* ; do
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