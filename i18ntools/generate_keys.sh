#!/bin/sh

ALL_MODULES='untangle-vm untangle-libuvm untangle-apache2-config untangle-net-alpaca
    untangle-casing-mail untangle-base-virus untangle-base-webfilter
    untangle-node-phish untangle-node-spyware untangle-node-spamassassin untangle-node-shield
    untangle-node-protofilter untangle-node-ips untangle-node-firewall untangle-node-reporting 
    untangle-node-openvpn untangle-node-adconnector untangle-node-boxbackup untangle-node-portal 
    untangle-node-pcremote untangle-node-adblocker untangle-node-cpd
    untangle-node-faild untangle-node-splitd untangle-node-sitefilter'
OFFICIAL_LANGUAGES='es zh pt_BR'

function update_keys()
{
case "$1" in
"untangle-vm")
    cd ../uvm/po/
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $1
    ;;
"untangle-libuvm")
    cd ../uvm-lib/po
    echo 'get main keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../servlets/webui/root/script/main.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../servlets/webui/root/script/components.js
    #general wizard
    xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../../../pkgs/untangle-apache2-config/files/var/www/script/wizard.js
    #setup wizard
    xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../servlets/setup/root/script/setup.js
    echo 'get config keys'
    xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../servlets/webui/root/script/config/*.js

    xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../servlets/reports/root/script/*.js

    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../servlets/alpaca/src/com/untangle/uvm/servlet/alpaca/ProxyServlet.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../localapi/com/untangle/uvm/util/OutsideValve.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../localapi/com/untangle/uvm/util/ReportingOutsideAccessValve.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../bootstrap/com/untangle/uvm/engine/UvmErrorReportValve.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../impl/com/untangle/uvm/engine/MailSenderImpl.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../api/com/untangle/uvm/BrandingBaseSettings.java

    echo 'get blingers keys'
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../impl/com/untangle/uvm/engine/Dispatcher.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../localapi/com/untangle/uvm/vnet/NodeBase.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../../firewall/impl/com/untangle/node/firewall/FirewallImpl.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../../ips/impl/com/untangle/node/ips/IpsNodeImpl.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../../openvpn/impl/com/untangle/node/openvpn/VpnNodeImpl.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../../protofilter/impl/com/untangle/node/protofilter/ProtoFilterImpl.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../../spam-base/impl/com/untangle/node/spam/SpamNodeImpl.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../../spyware/impl/com/untangle/node/spyware/SpywareImpl.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../../virus-base/impl/com/untangle/node/virus/VirusNodeImpl.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../../webfilter-base/impl/com/untangle/node/webfilter/WebFilterBase.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../../../../hades/rup/faild/impl/com/untangle/node/faild/FailDImpl.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../../../../hades/rup/splitd/impl/com/untangle/node/splitd/SplitDImpl.java 
    find ../../uvm/hier -name '*.py' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot

    msgcat tmp_keys.pot fmt_keys.pot -o tmp_keys.pot
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $1
    ;;
"untangle-apache2-config")
    cd ../../pkgs/untangle-apache2-config/po
    xgettext --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot ../*.py
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $1
    ;;
"untangle-net-alpaca")
    cd ../../pkgs/untangle-net-alpaca/po
    # choose a specific file to create the pot file; then we join messages to that file
    xgettext --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot ../files/var/lib/rails/untangle-net-alpaca/app/controllers/application.rb -o tmp_keys.pot
    find ../files/var/lib/rails/untangle-net-alpaca/app -name "*.rb" -exec xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot '{}' \;
    find ../files/var/lib/rails/untangle-net-alpaca/public/javascripts/pages -name "*.js" -exec xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot '{}' \;
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $1
    ;;
"untangle-base-webfilter")
    moduleName=`echo "$1"|cut -d"-" -f3`
    cd ../webfilter-base/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/${1}/settings.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../api/com/untangle/node/webfilter/BlockPageServlet.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../../http-casing/localapi/com/untangle/node/http/BlockPageUtil.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../impl/com/untangle/node/webfilter/*.java
    find ../hier -name '*.py' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot
    ruby ../../i18ntools/xi18ntags.rb ../../uvm-lib/servlets/blockpage/root/blockpage_template.jspx >> ./tmp_keys.pot
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $1
    ;;
"untangle-node-phish")
    moduleName=`echo "$1"|cut -d"-" -f3`
    cd ../${moduleName}/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/${1}/settings.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../servlets/idblocker/src/com/untangle/node/phish/BlockPageServlet.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../../http-casing/localapi/com/untangle/node/http/BlockPageUtil.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../../spam-base/impl/com/untangle/node/spam/*.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../impl/com/untangle/node/${moduleName}/*.java
    find ../hier -name '*.py' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot
    ruby ../../i18ntools/xi18ntags.rb ../../uvm-lib/servlets/blockpage/root/blockpage_template.jspx >> ./tmp_keys.pot
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $1
    ;;
"untangle-node-spyware")
    moduleName=`echo "$1"|cut -d"-" -f3`
    cd ../${moduleName}/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/${1}/settings.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../servlets/spyware/src/com/untangle/node/spyware/BlockPageServlet.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../../http-casing/localapi/com/untangle/node/http/BlockPageUtil.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../impl/com/untangle/node/${moduleName}/*.java
    find ../hier -name '*.py' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot
    ruby ../../i18ntools/xi18ntags.rb ../../uvm-lib/servlets/blockpage/root/blockpage_template.jspx >> ./tmp_keys.pot
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $1
    ;;
"untangle-node-shield"|"untangle-node-protofilter"|"untangle-node-ips"|"untangle-node-firewall"|"untangle-node-reporting"|untangle-node-adblocker|untangle-node-cpd)
    moduleName=`echo "$1"|cut -d"-" -f3`
    cd ../${moduleName}/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/${1}/settings.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../impl/com/untangle/node/${moduleName}/*.java
    find ../hier -name '*.py' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $1
    ;;
"untangle-node-spamassassin")
    moduleName=`echo "$1"|cut -d"-" -f3`
    cd ../${moduleName}/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/${1}/settings.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../../spam-base/impl/com/untangle/node/spam/*.java
    find ../hier -name '*.py' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $1
    ;;
"untangle-node-openvpn")
    moduleName=`echo "$1"|cut -d"-" -f3`
    cd ../${moduleName}/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/${1}/settings.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../impl/com/untangle/node/${moduleName}/*.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../impl/com/untangle/node/${moduleName}/*.java
    #xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../../../pkgs/untangle-apache2-config/files/var/www/script/wizard.js
    find ../hier -name '*.py' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $1
    ;;
"untangle-node-adconnector")
    moduleName=`echo "$1"|cut -d"-" -f3`
    cd ../../../hades/rup/${moduleName}/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/${1}/settings.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../impl/com/untangle/node/${moduleName}/*.java
    find ../hier -name '*.py' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot
    ruby ../../../../work/src/i18ntools/xi18ntags.rb ../servlets/adpb/root/index.jsp dup>> ./tmp_keys.pot
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $1
    ;;
"untangle-node-boxbackup")
    moduleName=`echo "$1"|cut -d"-" -f3`
    cd ../../../hades/rup/${moduleName}/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/${1}/settings.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../impl/com/untangle/node/${moduleName}/*.java
    find ../hier -name '*.py' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $1
    ;;
"untangle-node-splitd")
    moduleName=`echo "$1"|cut -d"-" -f3`
    cd ../../../hades/rup/${moduleName}/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/${1}/settings.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../impl/com/untangle/node/${moduleName}/*.java
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $1
    ;;
"untangle-node-faild")
    moduleName=`echo "$1"|cut -d"-" -f3`
    cd ../../../hades/rup/${moduleName}/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/${1}/settings.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../impl/com/untangle/node/${moduleName}/*.java
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $1
    ;;

"untangle-node-portal")
    moduleName=`echo "$1"|cut -d"-" -f3`
    cd ../../../hades/rup/${moduleName}/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/${1}/settings.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../servlets/browser/src/com/untangle/node/portal/browser/CommandRunner.java
#    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../servlets/proxy/src/com/untangle/node/portal/proxy/ForwardServlet.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../servlets/proxy/src/com/untangle/node/portal/proxy/WebProxy.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../common/login/error.jsp
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../common/login/login.jsp
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../impl/com/untangle/node/${moduleName}/*.java
    find ../hier -name '*.py' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $1
    ;;
"untangle-node-pcremote")
    moduleName=`echo "$1"|cut -d"-" -f3`
    cd ../../../hades/rup/${moduleName}/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/${1}/settings.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../servlets/rsa/src/com/untangle/node/rsa/RsaServlet.java
#    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../servlets/rsaproxy/src/com/untangle/node/rsa/proxy/ForwardServlet.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../servlets/rsaproxy/src/com/untangle/node/rsa/proxy/WebProxy.java
    find ../hier -name '*.py' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot
    ruby ../../../../work/src/i18ntools/xi18ntags.rb ../servlets/rsa/root/rsa.jspx dup >> ./tmp_jspx_keys.pot
    msgcat tmp_keys.pot tmp_jspx_keys.pot -o tmp_keys.pot
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    rm tmp_jspx_keys.pot
    update_po $1
    ;;
"untangle-casing-mail")
    cd ../mail-casing/po/
    xgettext --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../servlets/quarantine/root/quarantine.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../servlets/quarantine/root/remaps.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../servlets/quarantine/root/request.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../servlets/quarantine/root/safelist.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../impl/com/untangle/node/mail/impl/quarantine/Quarantine.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ktr -o tmp_keys.pot ../impl/resources/com/untangle/node/mail/impl/quarantine/DigestSimpleEmail_HTML.vm
    find ../hier -name '*.py' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot
    ruby ../../i18ntools/xi18ntags.rb ../servlets/quarantine/root/inbox.jspx >> ./tmp_keys.pot
    ruby ../../i18ntools/xi18ntags.rb ../servlets/quarantine/root/request.jspx >> ./tmp_keys.pot
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $1
    ;;
"untangle-base-virus")
    cd ../virus-base/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/$1/settings.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../servlets/virus/src/com/untangle/node/virus/BlockPageServlet.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../../http-casing/localapi/com/untangle/node/http/BlockPageUtil.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../impl/com/untangle/node/virus/*.java
    find ../hier -name '*.py' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot
    ruby ../../i18ntools/xi18ntags.rb ../../uvm-lib/servlets/blockpage/root/blockpage_template.jspx >> ./tmp_keys.pot
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $1
    ;;
"untangle-node-sitefilter")
    moduleName=`echo "$1"|cut -d"-" -f3`
    cd ../../../hades/rup/${moduleName}/po/
    echo 'get new keys'
    xgettext --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot ../hier/usr/share/untangle/web/webui/script/${1}/settings.js
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot ../servlets/sitefilter/src/com/untangle/node/sitefilter/BlockPageServlet.java
    xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot ../impl/com/untangle/node/${moduleName}/*.java
    find ../hier -name '*.py' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot
    msgmerge -U $1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $1
    ;;
"untangle-systray")
    pygettext ../../../internal/isotools/wintangle-systray/*.py
    ;;
*)
    echo 1>&2 Module Name \"$1\" is invalid ...
    exit 127
    ;;
esac
}

function update_po( )
{
    echo 'update po files'
    for lang in ${OFFICIAL_LANGUAGES}
    do
        pot=$1.pot
        po=${lang}/$1.po
        if [ ! -e $po ]; then
            mkdir -p $(dirname $po)
            touch $po
        fi
        msgmerge -U $po $pot
    done
}


if [ $# -ne 1 ]; then
     echo 1>&2 Usage: $0 "<module_name | all>"
     exit 127
fi

if [ $1 == 'all' ]
then

    current_dir=`pwd`
    for module in ${ALL_MODULES}
    do
        echo 'Updating keys for '${module}'...'
        update_keys ${module}
        cd ${current_dir}
    done

else
    update_keys $1
fi


# All done, exit ok
exit 0