#!/bin/bash
OFFICIAL_LANGUAGES='de es fr ja pt_BR zh_CN'


for lang in ${OFFICIAL_LANGUAGES}
do
    for module in untangle-node-phish untangle-node-spamassassin untangle-node-shield untangle-node-protofilter untangle-node-ips untangle-node-firewall untangle-node-reporting untangle-node-openvpn untangle-node-adblocker
    do
        module_dir=`echo "${module}"|cut -d"-" -f3`
        newpofile=./langcheckin/${lang}/${module}.po
        pofiledir=../${module_dir}/po/${lang}/
        if [ -e ${newpofile} ]
        then
            cp ${newpofile} ${pofiledir}
        fi
    done
    for module in untangle-node-adconnector untangle-node-bandwidth untangle-node-boxbackup untangle-node-branding untangle-node-spamblocker untangle-node-virusblocker untangle-node-faild untangle-node-license untangle-node-policy untangle-node-sitefilter untangle-node-faild untangle-node-splitd untangle-node-webcache untangle-node-ipsec untangle-node-classd
    do
        module_dir=`echo "${module}"|cut -d"-" -f3`
        newpofile=./langcheckin/${lang}/${module}.po
        pofiledir=../../../hades/src/${module_dir}/po/${lang}/
        if [ -e ${newpofile} ]
        then
            cp ${newpofile} ${pofiledir}
        fi
    done

    # Handle those with names different than directory
    newpofile=../langcheckin/${lang}/untangle-casing-smtp.po
    pofiledir=../smtp-casing/po/${lang}/
    if [ -e ${newpofile} ]
    then
        cp ${newpofile} ${pofiledir}
    fi

    newpofile=../langcheckin/${lang}/untangle-base-webfilter.po
    pofiledir=../webfilter-base/po/${lang}/
    if [ -e ${newpofile} ]
    then
        cp ${newpofile} ${pofiledir}
    fi

    newpofile=../langcheckin/${lang}/untangle-libuvm.po
    pofiledir=../uvm/po/${lang}/
    if [ -e ${newpofile} ]
    then
        cp ${newpofile} ${pofiledir}
    fi

    newpofile=../langcheckin/${lang}/untangle-base-virus.po
    pofiledir=../virus-base/po/${lang}/
    if [ -e ${newpofile} ]
    then
        cp ${newpofile} ${pofiledir}
    fi

    newpofile=../langcheckin/${lang}/untangle-apache2-config.po
    pofiledir=../../pkgs/untangle-apache2-config/po/${lang}/
    if [ -e ${newpofile} ]
    then
        cp ${newpofile} ${pofiledir}
    fi

done

# cd work/src
# svn commit -m "Latest community translations - lite" ./shield/po/nl/untangle-node-shield.po ./shield/po/zh_CN/untangle-node-shield.po ./shield/po/es/untangle-node-shield.po ./shield/po/ja/untangle-node-shield.po ./shield/po/zh/untangle-node-shield.po ./shield/po/pt_BR/untangle-node-shield.po ./shield/po/fr/untangle-node-shield.po ./shield/po/de/untangle-node-shield.po ./smtp-casing/po/nl/untangle-casing-smtp.po ./smtp-casing/po/zh_CN/untangle-casing-smtp.po ./smtp-casing/po/es/untangle-casing-smtp.po ./smtp-casing/po/ja/untangle-casing-smtp.po ./smtp-casing/po/zh/untangle-casing-smtp.po ./smtp-casing/po/pt_BR/untangle-casing-smtp.po ./smtp-casing/po/fr/untangle-casing-smtp.po ./smtp-casing/po/de/untangle-casing-smtp.po ./adblocker/po/zh_CN/untangle-node-adblocker.po ./adblocker/po/es/untangle-node-adblocker.po ./adblocker/po/ja/untangle-node-adblocker.po ./adblocker/po/zh/untangle-node-adblocker.po ./adblocker/po/pt_BR/untangle-node-adblocker.po ./adblocker/po/fr/untangle-node-adblocker.po ./adblocker/po/de/untangle-node-adblocker.po ./phish/po/nl/untangle-node-phish.po ./phish/po/zh_CN/untangle-node-phish.po ./phish/po/es/untangle-node-phish.po ./phish/po/ja/untangle-node-phish.po ./phish/po/zh/untangle-node-phish.po ./phish/po/pt_BR/untangle-node-phish.po ./phish/po/fr/untangle-node-phish.po ./phish/po/de/untangle-node-phish.po ./protofilter/po/nl/untangle-node-protofilter.po ./protofilter/po/zh_CN/untangle-node-protofilter.po ./protofilter/po/es/untangle-node-protofilter.po ./protofilter/po/ja/untangle-node-protofilter.po ./protofilter/po/zh/untangle-node-protofilter.po ./protofilter/po/pt_BR/untangle-node-protofilter.po ./protofilter/po/fr/untangle-node-protofilter.po ./protofilter/po/de/untangle-node-protofilter.po ./webfilter-base/po/nl/untangle-base-webfilter.po ./webfilter-base/po/zh_CN/untangle-base-webfilter.po ./webfilter-base/po/es/untangle-base-webfilter.po ./webfilter-base/po/ja/untangle-base-webfilter.po ./webfilter-base/po/zh/untangle-base-webfilter.po ./webfilter-base/po/pt_BR/untangle-base-webfilter.po ./webfilter-base/po/fr/untangle-base-webfilter.po ./webfilter-base/po/de/untangle-base-webfilter.po ./uvm/po/nl/untangle-libuvm.po ./uvm/po/zh_CN/untangle-libuvm.po ./uvm/po/es/untangle-libuvm.po ./uvm/po/ja/untangle-libuvm.po ./uvm/po/zh/untangle-libuvm.po ./uvm/po/pt_BR/untangle-libuvm.po ./uvm/po/fr/untangle-libuvm.po ./uvm/po/de/untangle-libuvm.po ./ips/po/nl/untangle-node-ips.po ./ips/po/zh_CN/untangle-node-ips.po ./ips/po/es/untangle-node-ips.po ./ips/po/ja/untangle-node-ips.po ./ips/po/zh/untangle-node-ips.po ./ips/po/pt_BR/untangle-node-ips.po ./ips/po/fr/untangle-node-ips.po ./ips/po/de/untangle-node-ips.po ./spamassassin/po/nl/untangle-node-spamassassin.po ./spamassassin/po/zh_CN/untangle-node-spamassassin.po ./spamassassin/po/es/untangle-node-spamassassin.po ./spamassassin/po/ja/untangle-node-spamassassin.po ./spamassassin/po/zh/untangle-node-spamassassin.po ./spamassassin/po/pt_BR/untangle-node-spamassassin.po ./spamassassin/po/fr/untangle-node-spamassassin.po ./spamassassin/po/de/untangle-node-spamassassin.po ./openvpn/po/nl/untangle-node-openvpn.po ./openvpn/po/zh_CN/untangle-node-openvpn.po ./openvpn/po/es/untangle-node-openvpn.po ./openvpn/po/ja/untangle-node-openvpn.po ./openvpn/po/zh/untangle-node-openvpn.po ./openvpn/po/pt_BR/untangle-node-openvpn.po ./openvpn/po/fr/untangle-node-openvpn.po ./openvpn/po/de/untangle-node-openvpn.po ./reporting/po/nl/untangle-node-reporting.po ./reporting/po/zh_CN/untangle-node-reporting.po ./reporting/po/es/untangle-node-reporting.po ./reporting/po/ja/untangle-node-reporting.po ./reporting/po/zh/untangle-node-reporting.po ./reporting/po/pt_BR/untangle-node-reporting.po ./reporting/po/fr/untangle-node-reporting.po ./reporting/po/de/untangle-node-reporting.po ./firewall/po/nl/untangle-node-firewall.po ./firewall/po/zh_CN/untangle-node-firewall.po ./firewall/po/es/untangle-node-firewall.po ./firewall/po/ja/untangle-node-firewall.po ./firewall/po/zh/untangle-node-firewall.po ./firewall/po/pt_BR/untangle-node-firewall.po ./firewall/po/fr/untangle-node-firewall.po ./firewall/po/de/untangle-node-firewall.po ./virus-base/po/nl/untangle-base-virus.po ./virus-base/po/zh_CN/untangle-base-virus.po ./virus-base/po/es/untangle-base-virus.po ./virus-base/po/ja/untangle-base-virus.po ./virus-base/po/zh/untangle-base-virus.po ./virus-base/po/pt_BR/untangle-base-virus.po ./virus-base/po/fr/untangle-base-virus.po ./virus-base/po/de/untangle-base-virus.po

# cd hades/src
# svn commit -m "Latest community translations - prem" ./classd/po/zh_CN/untangle-node-classd.po ./classd/po/es/untangle-node-classd.po ./classd/po/ja/untangle-node-classd.po ./classd/po/pt_BR/untangle-node-classd.po ./classd/po/fr/untangle-node-classd.po ./classd/po/de/untangle-node-classd.po ./license/po/zh_CN/untangle-node-license.po ./license/po/es/untangle-node-license.po ./license/po/ja/untangle-node-license.po ./license/po/pt_BR/untangle-node-license.po ./license/po/fr/untangle-node-license.po ./license/po/de/untangle-node-license.po ./bandwidth/po/zh_CN/untangle-node-bandwidth.po ./bandwidth/po/es/untangle-node-bandwidth.po ./bandwidth/po/ja/untangle-node-bandwidth.po ./bandwidth/po/pt_BR/untangle-node-bandwidth.po ./bandwidth/po/fr/untangle-node-bandwidth.po ./bandwidth/po/de/untangle-node-bandwidth.po ./splitd/po/nl/untangle-node-splitd.po ./splitd/po/zh_CN/untangle-node-splitd.po ./splitd/po/es/untangle-node-splitd.po ./splitd/po/ja/untangle-node-splitd.po ./splitd/po/zh/untangle-node-splitd.po ./splitd/po/pt_BR/untangle-node-splitd.po ./splitd/po/fr/untangle-node-splitd.po ./splitd/po/de/untangle-node-splitd.po ./faild/po/nl/untangle-node-faild.po ./faild/po/zh_CN/untangle-node-faild.po ./faild/po/es/untangle-node-faild.po ./faild/po/ja/untangle-node-faild.po ./faild/po/zh/untangle-node-faild.po ./faild/po/pt_BR/untangle-node-faild.po ./faild/po/fr/untangle-node-faild.po ./faild/po/de/untangle-node-faild.po ./webcache/po/zh_CN/untangle-node-webcache.po ./webcache/po/es/untangle-node-webcache.po ./webcache/po/ja/untangle-node-webcache.po ./webcache/po/pt_BR/untangle-node-webcache.po ./webcache/po/fr/untangle-node-webcache.po ./webcache/po/de/untangle-node-webcache.po ./ipsec/po/zh_CN/untangle-node-ipsec.po ./ipsec/po/es/untangle-node-ipsec.po ./ipsec/po/ja/untangle-node-ipsec.po ./ipsec/po/pt_BR/untangle-node-ipsec.po ./ipsec/po/fr/untangle-node-ipsec.po ./ipsec/po/de/untangle-node-ipsec.po ./boxbackup/po/nl/untangle-node-boxbackup.po ./boxbackup/po/zh_CN/untangle-node-boxbackup.po ./boxbackup/po/es/untangle-node-boxbackup.po ./boxbackup/po/ja/untangle-node-boxbackup.po ./boxbackup/po/zh/untangle-node-boxbackup.po ./boxbackup/po/pt_BR/untangle-node-boxbackup.po ./boxbackup/po/ga/untangle-node-boxbackup.po ./boxbackup/po/fr/untangle-node-boxbackup.po ./boxbackup/po/de/untangle-node-boxbackup.po ./spamblocker/po/nl/untangle-node-spamblocker.po ./spamblocker/po/zh_CN/untangle-node-spamblocker.po ./spamblocker/po/es/untangle-node-spamblocker.po ./spamblocker/po/ja/untangle-node-spamblocker.po ./spamblocker/po/zh/untangle-node-spamblocker.po ./spamblocker/po/pt_BR/untangle-node-spamblocker.po ./spamblocker/po/fr/untangle-node-spamblocker.po ./spamblocker/po/de/untangle-node-spamblocker.po ./adconnector/po/nl/untangle-node-adconnector.po ./adconnector/po/zh_CN/untangle-node-adconnector.po ./adconnector/po/es/untangle-node-adconnector.po ./adconnector/po/ja/untangle-node-adconnector.po ./adconnector/po/zh/untangle-node-adconnector.po ./adconnector/po/pt_BR/untangle-node-adconnector.po ./adconnector/po/ga/untangle-node-adconnector.po ./adconnector/po/fr/untangle-node-adconnector.po ./adconnector/po/de/untangle-node-adconnector.po ./sitefilter/po/zh_CN/untangle-node-sitefilter.po ./sitefilter/po/es/untangle-node-sitefilter.po ./sitefilter/po/ja/untangle-node-sitefilter.po ./sitefilter/po/zh/untangle-node-sitefilter.po ./sitefilter/po/pt_BR/untangle-node-sitefilter.po ./sitefilter/po/fr/untangle-node-sitefilter.po ./sitefilter/po/de/untangle-node-sitefilter.po ./policy/po/zh_CN/untangle-node-policy.po ./policy/po/es/untangle-node-policy.po ./policy/po/ja/untangle-node-policy.po ./policy/po/pt_BR/untangle-node-policy.po ./policy/po/fr/untangle-node-policy.po ./policy/po/de/untangle-node-policy.po ./support/po/zh_CN/untangle-node-support.po ./support/po/es/untangle-node-support.po ./support/po/ja/untangle-node-support.po ./support/po/pt_BR/untangle-node-support.po ./support/po/fr/untangle-node-support.po ./support/po/de/untangle-node-support.po ./branding/po/zh_CN/untangle-node-branding.po ./branding/po/es/untangle-node-branding.po ./branding/po/ja/untangle-node-branding.po ./branding/po/pt_BR/untangle-node-branding.po ./branding/po/fr/untangle-node-branding.po ./branding/po/de/untangle-node-branding.po ./https-casing/po/zh_CN/untangle-casing-https.po ./https-casing/po/es/untangle-casing-https.po ./https-casing/po/ja/untangle-casing-https.po ./https-casing/po/pt_BR/untangle-casing-https.po ./https-casing/po/fr/untangle-casing-https.po ./https-casing/po/de/untangle-casing-https.po