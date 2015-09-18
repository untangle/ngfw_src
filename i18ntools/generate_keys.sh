#!/bin/bash
# untangle-node-webfiter and untangle-node-virus-blocker are not needed since they rely on base-web-filter and base-virus-blocker
ALL_MODULES='untangle-vm untangle-libuvm untangle-apache2-config untangle-casing-smtp
    untangle-base-virus-blocker untangle-base-web-filter untangle-node-ad-blocker
    untangle-node-firewall
    untangle-node-intrusion-prevention untangle-node-openvpn untangle-node-phish-blocker
    untangle-node-application-control-lite untangle-node-reporting
    untangle-node-spam-blocker-lite
    untangle-node-directory-connector untangle-node-bandwidth untangle-node-configuration-backup
    untangle-node-branding-manager untangle-node-spam-blocker
<<<<<<< .mine
    untangle-node-faild untangle-node-ipsec-vpn
=======
    untangle-node-wan-failover untangle-node-ipsec
>>>>>>> .r41260
    untangle-node-policy-manager untangle-node-web-filter untangle-node-wan-balancer
    untangle-node-live-support untangle-node-webcache untangle-node-classd
    untangle-node-captive-portal untangle-casing-ssl'
OFFICIAL_LANGUAGES='de es fr ja pt_BR zh_CN xx'

WORK=~/work
HADES=~/hades

mkdir -p /tmp/language
cd /tmp/language

function update_keys()
{
case "$1" in

"untangle-vm")
# TODO Find out what happend with uvm folder
#    cd ../uvm/po/
#    msgmerge -U -N $1.pot tmp_keys.pot
#    rm tmp_keys.pot
#    update_po $1
    ;;
"untangle-libuvm")
    # uvm javascript. find all _("string")
    find $WORK/src/uvm/servlets -name '*.js' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot
    find $WORK -wholename '*/web/*.js' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot
    find $HADES -wholename '*/web/*.js' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot

    # untangle-apache2-config javascript. find all _("string")
    find $WORK/pkgs/untangle-apache2-config/files/var/www/script -name '*.js' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot
    xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot $WORK/pkgs/untangle-apache2-config/files/usr/lib/python*/*.py
    xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot $WORK/pkgs/untangle-apache2-config/files/usr/share/untangle/mod_python/error/*.py
    xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot $WORK/pkgs/untangle-apache2-config/files/usr/share/untangle/mod_python/auth/*.py

    # uvm java. find all tr("string")
    find $WORK/src/uvm -name '*.java' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot
    # uvm java. find all marktr("string")
    find $WORK/src/uvm -name '*.java' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot

    # python scripts
    find $WORK/src -name '*.py' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot
    find $HADES/src -name '*.py' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot

    # all report/event entries
    rm -f /tmp/strings
    find $WORK/src -type f -wholename '*reports/*.js' | xargs cat | egrep '(description|title)' >> /tmp/strings
    find $HADES/src -type f -wholename '*reports/*.js' | xargs cat | egrep '(description|title)' >> /tmp/strings
    find $WORK/src -type f -wholename '*events/*.js' | xargs cat | egrep '(description|title)' >> /tmp/strings
    find $HADES/src -type f -wholename '*events/*.js' | xargs cat | egrep '(description|title)' >> /tmp/strings
    xgettext -j --copyright-holder='Untangle, Inc.' -a -o tmp_keys.pot /tmp/strings

    # java files
    find $WORK/src -type f -name '*.java' | grep -v '.*/downloads/.*' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot
    find $HADES/src -type f -name '*.java' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot

    # jspx files
    find $WORK/src/uvm/servlets -type f -name '*.jspx' | xargs ruby $WORK/src/i18ntools/xi18ntags.rb >> ./tmp_keys.pot

    msgcat tmp_keys.pot $WORK/src/uvm/po/fmt_keys.pot -o tmp_keys.pot
    msgmerge -U -N  $WORK/src/uvm/po/$1.pot tmp_keys.pot
    update_po $WORK/src/uvm/po $1
    ;;
"untangle-apache2-config")
    xgettext --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot $WORK/pkgs/untangle-apache2-config/files/usr/lib/python*/*.py
    xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot $WORK/pkgs/untangle-apache2-config/files/usr/share/untangle/mod_python/error/*.py
    xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot $WORK/pkgs/untangle-apache2-config/files/usr/share/untangle/mod_python/auth/*.py
    msgmerge -U -N $WORK/pkgs/untangle-apache2-config/po/$1.pot tmp_keys.pot
    update_po $WORK/pkgs/untangle-apache2-config/po $1
    ;;
<<<<<<< .mine
"untangle-node-application-control-lite"|"untangle-node-intrusion-prevention"|"untangle-node-firewall"|"untangle-node-reporting"|"untangle-node-ad-blocker"|"untangle-node-spam-blocker-lite"|"untangle-node-captive-portal"|"untangle-base-web-filter"|"untangle-node-phish-blocker"|"untangle-node-openvpn"|"untangle-node-directory-connector"|"untangle-node-bandwidth"|"untangle-node-configuration-backup"|"untangle-node-faild"|"untangle-node-policy-manager"|"untangle-node-faild"|"untangle-node-wan-balancer"|"untangle-node-webcache"|"untangle-node-web-filter"|"untangle-node-spam-blocker"|"untangle-node-classd"|"untangle-node-branding-manager"|"untangle-node-ipsec-vpn"|"untangle-node-live-support"|"untangle-casing-ssl"|"untangle-casing-smtp"|"untangle-base-virus-blocker")
=======
"untangle-node-application-control-lite"|"untangle-node-intrusion-prevention"|"untangle-node-firewall"|"untangle-node-reporting"|"untangle-node-ad-blocker"|"untangle-node-spam-blocker-lite"|"untangle-node-captive-portal"|"untangle-base-web-filter"|"untangle-node-phish-blocker"|"untangle-node-openvpn"|"untangle-node-directory-connector"|"untangle-node-bandwidth"|"untangle-node-configuration-backup"|"untangle-node-wan-failover"|"untangle-node-policy-manager"|"untangle-node-wan-failover"|"untangle-node-wan-balancer"|"untangle-node-webcache"|"untangle-node-web-filter"|"untangle-node-spam-blocker"|"untangle-node-classd"|"untangle-node-branding-manager"|"untangle-node-ipsec"|"untangle-node-live-support"|"untangle-casing-ssl"|"untangle-casing-smtp"|"untangle-base-virus-blocker")
>>>>>>> .r41260
    app=`echo "$1"|cut -d"-" -f3`
    mid=`echo "$1"|cut -d"-" -f2`
    moduleName=""
    if [ $mid == "base" ] ; then
        moduleName="$app-base"
    elif [ $mid == "casing" ] ; then
        moduleName="$app-casing"
    else
        moduleName="$app"
    fi

    if [ -d $WORK/src/$moduleName ] ; then
        DIR=$WORK/src/$moduleName
    fi
    if [ -d $HADES/src/$moduleName ] ; then
        DIR=$HADES/src/$moduleName
    fi

    # java 
    find $DIR -type f -name '*.java' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot
    find $DIR -type f -name '*.java' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot

    # http casing
    if [ $1 = "untangle-base-web-filter" ] || [ $1 == "untangle-node-web-filter" ] || [ $1 = "untangle-base-virus-blocker" ] ; then
        # block page
        xgettext -j --copyright-holder='Untangle, Inc.' -L Java -ktr -o tmp_keys.pot $WORK/src/http-casing/src/com/untangle/node/http/BlockPageUtil.java

        # jspx
        find $WORK/src/uvm/servlets -type f -name '*.jspx' | xargs ruby $WORK/src/i18ntools/xi18ntags.rb >> ./tmp_keys.pot
    fi
    # spam-blocker-base
    if [ $1 = "untangle-node-phish-blocker" ] || [ $1 = "untangle-node-spam-blocker-lite" ] || [ $1 = "untangle-node-spam-blocker" ] ; then
        xgettext -j --copyright-holder='Untangle, Inc.' -L Java -kmarktr -o tmp_keys.pot $WORK/src/spam-blocker-base/src/com/untangle/node/spam/*.java
    fi

    # python
    find $DIR -type f -name '*.py' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -k_ -o tmp_keys.pot

    # javascript
    find $DIR/hier/usr/share/untangle/web -type f -name '*.js' | xargs xgettext -j --copyright-holder='Untangle, Inc.' -L Python -ki18n._ -o tmp_keys.pot

    # reports & event entries
    # rm -f /tmp/strings
    # find $DIR/hier/usr/share/untangle/ -type f -wholename '*reports/*.js' | xargs cat | egrep '(description|title)' >> /tmp/strings
    # find $DIR/hier/usr/share/untangle/ -type f -wholename '*events/*.js' | xargs cat | egrep '(description|title)' >> /tmp/strings
    # xgettext -j --copyright-holder='Untangle, Inc.' -a -o tmp_keys.pot /tmp/strings

    msgmerge -U -N $DIR/po/$1.pot tmp_keys.pot
    rm tmp_keys.pot
    update_po $DIR/po $1 
    ;;
*)
    echo 1>&2 Module Name \"$1\" is invalid ...
    exit 127
    ;;
esac
}

function update_po( )
{
    dir=$1
    name=$2
    echo "Updating PO file: $dir/$name.po"
    for lang in ${OFFICIAL_LANGUAGES}
    do
        pot=$dir/$name.pot
        po=$dir/$lang/$name.po
        if [ ! -e $po ]; then
            mkdir -p $(dirname $po)
            touch $po
        fi
        #msgattrib --no-fuzzy -o $po $po
        msgmerge -U -N $po $pot
        sed -i -e '/^#.*/d' $po
        # If its the test language, go ahead and set the values in the po file
        if [ $lang == "xx" ] ; then
            $WORK/src/i18ntools/set_test_language_strings.py $po > /tmp/xx.po
            cp /tmp/xx.po $po
        fi
            
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
        rm -f tmp_keys.pot ; touch tmp_keys.pot
        update_keys ${module}
        cd ${current_dir}
    done

else
    rm -f tmp_keys.pot ; touch tmp_keys.pot
    update_keys $1
fi


# All done, exit ok
exit 0
