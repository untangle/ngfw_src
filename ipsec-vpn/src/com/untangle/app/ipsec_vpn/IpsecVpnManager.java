/**
 * $Id: IpsecVpnManager.java 40784 2015-07-27 18:44:40Z mahotz $
 */

package com.untangle.app.ipsec_vpn;

import java.util.LinkedList;
import java.util.Formatter;
import java.util.List;
import java.io.File;
import java.io.FileWriter;
import java.net.InetAddress;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONString;
import org.json.JSONObject;

import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.CertificateManager;
import com.untangle.uvm.UvmContextFactory;

public class IpsecVpnManager
{
    private final Logger logger = Logger.getLogger(getClass());

// THIS IS FOR ECLIPSE - @formatter:off

    private static final String TAB = "\t";
    private static final String RET = "\n";

    private static final String RELOAD_IPSEC_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-reload";
    private static final String XAUTH_UPDOWN_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-xauth-updown";
    private static final String IKEV2_UPDOWN_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-ikev2-updown";
    private static final String IPTABLES_GRE_SCRIPT = "/etc/untangle-netd/iptables-rules.d/712-gre";

    private static final String IPSEC_CONF_FILE = "/etc/ipsec.conf";
    private static final String IPSEC_SECRETS_FILE = "/etc/ipsec.secrets";
    private static final String OPTIONS_XL2TPD_FILE = "/etc/ppp/options.xl2tpd";
    private static final String XL2TPD_CONF_FILE = "/etc/xl2tpd/xl2tpd.conf";
    private static final String STRONGSWAN_CONF_FILE = "/etc/strongswan.conf";

    private static final String FILE_DISCLAIMER =  "# This file is created and maintained by the Untangle IPsec service." + RET +
                                                   "# If you modify this file manually, your changes will be overwritten!" + RET + RET;

    // these are the default values that will be used when phase 1/2 manual configuration is NOT enabled
    // and were chosen for maximum compatibility with our previous releases that used openswan

    private static final String ike_default = "3des-md5-modp2048,3des-md5-modp1536,3des-md5-modp1024," +
                                              "3des-sha1-modp2048,3des-sha1-modp1536,3des-sha1-modp1024," +
                                              "aes128-md5-modp2048,aes128-md5-modp1536,aes128-md5-modp1024," +
                                              "aes128-sha1-modp2048,aes128-sha1-modp1536,aes128-sha1-modp1024," +
                                              "aes256-md5-modp2048,aes256-md5-modp1536,aes256-md5-modp1024," +
                                              "aes256-sha1-modp2048,aes256-sha1-modp1536,aes256-sha1-modp1024";

    private static final String esp_default = "3des-md5-modp2048,3des-md5-modp1536,3des-md5-modp1024," +
                                              "3des-sha1-modp2048,3des-sha1-modp1536,3des-sha1-modp1024," +
                                              "aes128-md5-modp2048,aes128-md5-modp1536,aes128-md5-modp1024," +
                                              "aes128-sha1-modp2048,aes128-sha1-modp1536,aes128-sha1-modp1024," +
                                              "aes256-md5-modp2048,aes256-md5-modp1536,aes256-md5-modp1024," +
                                              "aes256-sha1-modp2048,aes256-sha1-modp1536,aes256-sha1-modp1024";

    private static final String ikelifetime_default = "1h";
    private static final String lifetime_default = "8h";

// THIS IS FOR ECLIPSE - @formatter:on

    public void generateConfig(IpsecVpnSettings settings)
    {
        logger.debug("generateConfig()");

        try {
            writeConfigFiles(settings);
            writeIptablesScript(settings);
            UvmContextFactory.context().execManager().exec(RELOAD_IPSEC_SCRIPT);
        }

        catch (Exception e) {
            logger.error("IpsecVpnManager.generateConfig()", e);
        }
    }

    private void writeConfigFiles(IpsecVpnSettings settings) throws Exception
    {
        logger.debug("writeConfigFiles()");

        String ipsecCrtFile = CertificateManager.CERT_STORE_PATH + UvmContextFactory.context().systemManager().getSettings().getIpsecCertificate().replaceAll("\\.pem", "\\.crt");
        String ipsecKeyFile = CertificateManager.CERT_STORE_PATH + UvmContextFactory.context().systemManager().getSettings().getIpsecCertificate().replaceAll("\\.pem", "\\.key");
        String domainName = UvmContextFactory.context().networkManager().getNetworkSettings().getDomainName();
        String hostName = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName();

        LinkedList<IpsecVpnTunnel> tunnelList = settings.getTunnels();
        LinkedList<VirtualListen> listenList = settings.getVirtualListenList();
        VirtualListen listen;
        IpsecVpnTunnel data;
        int x;

        FileWriter ipsec_conf = new FileWriter(IPSEC_CONF_FILE, false);
        FileWriter ipsec_secrets = new FileWriter(IPSEC_SECRETS_FILE, false);
        FileWriter options_xl2tpd = new FileWriter(OPTIONS_XL2TPD_FILE, false);
        FileWriter xl2tpd_conf = new FileWriter(XL2TPD_CONF_FILE, false);
        FileWriter strongswan_conf = new FileWriter(STRONGSWAN_CONF_FILE, false);

        AddressCalculator calculator = new AddressCalculator(settings.getVirtualAddressPool());
        String osArch = System.getProperty("os.arch", "unknown");

        ipsec_conf.write("# " + IPSEC_CONF_FILE + RET + FILE_DISCLAIMER);
        ipsec_secrets.write("# " + IPSEC_SECRETS_FILE + RET + FILE_DISCLAIMER);
        options_xl2tpd.write("# " + OPTIONS_XL2TPD_FILE + RET + FILE_DISCLAIMER);
        xl2tpd_conf.write("; " + XL2TPD_CONF_FILE + RET + FILE_DISCLAIMER.replaceAll("#", ";"));
        strongswan_conf.write("# " + STRONGSWAN_CONF_FILE + RET + FILE_DISCLAIMER);

        if (settings == null) {
            ipsec_conf.close();
            ipsec_secrets.close();
            options_xl2tpd.close();
            xl2tpd_conf.close();
            return;
        }

        // first we create the setup section in the ipsec.conf file
        // this section is required and contains general protocol
        // config directives and items that are common to all tunnels
        ipsec_conf.write("config setup" + RET);
        ipsec_conf.write(TAB + "uniqueids=yes" + RET);

        if (settings.getCharonDebug().length() > 0) {
            ipsec_conf.write(TAB + "charondebug=" + settings.getCharonDebug() + RET);
        }

        ipsec_conf.write(RET);

        // put the ipsec certificate key in the secrets file for IKEv2
        ipsec_secrets.write(": RSA " + ipsecKeyFile + RET);

        for (x = 0; x < tunnelList.size(); x++) {
            // for each active tunne we create a corresponding
            // section in the ipsec.conf file

            data = tunnelList.get(x);
            if (data.getActive() != true) continue;

            // Use the id and description to create a unique connection name that won't cause
            // problems in the ipsec.conf file by replacing non-word characters with a hyphen.
            // We also prefix this name with UT123_ to ensure no dupes in the config file.
            String workname = data.getDescription().replaceAll("\\W", "-");

            // use the IPMaskedAddress thingy to write the correct
            // network/prefix values in the ipsec.conf file
            IPMaskedAddress leftFixer = new IPMaskedAddress(data.getLeftSubnet());
            IPMaskedAddress rightFixer = new IPMaskedAddress(data.getRightSubnet());
            String leftString = leftFixer.getMaskedAddress().getHostAddress() + "/" + leftFixer.getPrefixLength();
            String rightString = rightFixer.getMaskedAddress().getHostAddress() + "/" + rightFixer.getPrefixLength();

            ipsec_conf.write("conn UT" + Integer.toString(data.getId()) + "_" + workname + RET);
            ipsec_conf.write(TAB + "keyexchange=ikev" + Integer.toString(data.getIkeVersion()) + RET);
            ipsec_conf.write(TAB + "type=" + data.getConntype() + RET);
            ipsec_conf.write(TAB + "authby=psk" + RET);

            ipsec_conf.write(TAB + "rekey=yes" + RET);
            ipsec_conf.write(TAB + "keyingtries=%forever" + RET);

            /*
             * When running on the ASUS we have to disable the TCP replay
             * protection, otherwise we see massive packet loss. The tunnel is
             * up but when testing with ping only 1 in 1000 or so replies are
             * actually received properly. Using tcpdump we see all the replies
             * and everything looks correct, but almost all seem to be ignored
             * by charon. Our best guess is the unique switch/network setup on
             * this device is somehow triggering the replay detection, so our
             * current solution is adding this config option.
             */
            if (osArch.equals("arm") == true) {
                ipsec_conf.write(TAB + "replay_window=0" + RET);
            }

            if (data.getPhase1Manual() == true) {
                ipsec_conf.write(TAB + "ike=" + data.getPhase1Cipher() + "-" + data.getPhase1Hash() + "-" + data.getPhase1Group() + "!" + RET);
                ipsec_conf.write(TAB + "ikelifetime=" + data.getPhase1Lifetime() + "s" + RET);
            } else {
                ipsec_conf.write(TAB + "ike=" + ike_default + RET);
                ipsec_conf.write(TAB + "ikelifetime=" + ikelifetime_default + RET);
            }

            if (data.getPhase2Manual() == true) {
                if (data.getPhase2Group().equals("disabled") == true) {
                    ipsec_conf.write(TAB + "esp=" + data.getPhase2Cipher() + "-" + data.getPhase2Hash() + "!" + RET);
                } else {
                    ipsec_conf.write(TAB + "esp=" + data.getPhase2Cipher() + "-" + data.getPhase2Hash() + "-" + data.getPhase2Group() + "!" + RET);
                }
                ipsec_conf.write(TAB + "lifetime=" + data.getPhase2Lifetime() + "s" + RET);
            } else {
                ipsec_conf.write(TAB + "esp=" + esp_default + RET);
                ipsec_conf.write(TAB + "lifetime=" + lifetime_default + RET);
            }

            if ((data.getDpddelay().equals("0") == false) && (data.getDpdtimeout().equals("0") == false)) {
                ipsec_conf.write(TAB + "dpddelay=" + data.getDpddelay() + RET);
                ipsec_conf.write(TAB + "dpdtimeout=" + data.getDpdtimeout() + RET);
                ipsec_conf.write(TAB + "dpdaction=restart" + RET);
            }

            ipsec_conf.write(TAB + "left=" + data.getLeft() + RET);

            // use the configured leftid if available otherwise use left
            if ((data.getLeftId() != null) && (data.getLeftId().length() > 0)) {
                ipsec_conf.write(TAB + "leftid=" + data.getLeftId() + RET);
            } else {
                ipsec_conf.write(TAB + "leftid=" + data.getLeft() + RET);
            }

            ipsec_conf.write(TAB + "leftsubnet=" + leftString + RET);
            ipsec_conf.write(TAB + "right=" + data.getRight() + RET);

            // use the configured rightid if available otherwise use right
            if ((data.getRightId() != null) && (data.getRightId().length() > 0)) {
                ipsec_conf.write(TAB + "rightid=" + data.getRightId() + RET);
            } else {
                ipsec_conf.write(TAB + "rightid=" + data.getRight() + RET);
            }

            ipsec_conf.write(TAB + "rightsubnet=" + rightString + RET);
            ipsec_conf.write(TAB + "auto=" + data.getRunmode() + RET);
            ipsec_conf.write(RET);

            // add the tunnel PSK to the ipsec.secrets file
            ipsec_secrets.write("# " + workname + RET);
            ipsec_secrets.write(data.getLeft() + " " + data.getRight() + " : PSK 0x" + StringHexify(data.getSecret()) + RET);

            // start with left but prefer leftid if not null and not empty
            String lid = data.getLeft();
            if ((data.getLeftId() != null) && (data.getLeftId().length() > 0)) lid = data.getLeftId();

            // start with right but prefer rightid if not null and not empty
            String rid = data.getRight();
            if ((data.getRightId() != null) && (data.getRightId().length() > 0)) rid = data.getRightId();

            // if lid != left or rid != right add another secret using those values
            if ((!data.getLeft().equals(lid)) || (!data.getRight().equals(rid))) {
                ipsec_secrets.write(lid + " " + rid + " : PSK 0x" + StringHexify(data.getSecret()) + RET);
            }
        }

        // if the L2TP/Xauth server is enabled then we create a config section
        // section for each one on each configured listen address
        if (settings.getVpnflag() == true) {
            for (x = 0; x < listenList.size(); x++) {
                listen = listenList.get(x);

                // -----------------------------------------------------------
                // create the L2TP config section for the interface
                // -----------------------------------------------------------

                ipsec_conf.write("conn VPN-L2TP-" + Integer.toString(x) + RET);
                ipsec_conf.write(TAB + "keyexchange=ikev1" + RET);
                ipsec_conf.write(TAB + "authby=psk" + RET);
                ipsec_conf.write(TAB + "auto=add" + RET);
                ipsec_conf.write(TAB + "keyingtries=3" + RET);
                ipsec_conf.write(TAB + "rekey=no" + RET);
                ipsec_conf.write(TAB + "ikelifetime=8h" + RET);
                ipsec_conf.write(TAB + "keylife=1h" + RET);
                ipsec_conf.write(TAB + "dpddelay=10" + RET);
                ipsec_conf.write(TAB + "dpdtimeout=90" + RET);
                ipsec_conf.write(TAB + "dpdaction=clear" + RET);
                ipsec_conf.write(TAB + "type=transport" + RET);
                ipsec_conf.write(TAB + "left=" + listen.getAddress() + RET);
                ipsec_conf.write(TAB + "leftprotoport=17/1701" + RET);
                ipsec_conf.write(TAB + "right=%any" + RET);
                ipsec_conf.write(TAB + "rightprotoport=17/%any" + RET);
                ipsec_conf.write(RET);

                // -----------------------------------------------------------
                // create the Xauth config section for the interface
                // -----------------------------------------------------------

                ipsec_conf.write("conn VPN-XAUTH-" + Integer.toString(x) + RET);
                ipsec_conf.write(TAB + "keyexchange=ikev1" + RET);

                if (settings.getAuthenticationType() == IpsecVpnSettings.AuthenticationType.LOCAL_DIRECTORY) {
                    ipsec_conf.write(TAB + "authby=xauthpsk" + RET);
                }

                if (settings.getAuthenticationType() == IpsecVpnSettings.AuthenticationType.RADIUS_SERVER) {
                    ipsec_conf.write(TAB + "leftauth=psk" + RET);
                    ipsec_conf.write(TAB + "rightauth=psk" + RET);
                    ipsec_conf.write(TAB + "rightauth2=xauth-radius" + RET);
                }

                ipsec_conf.write(TAB + "forceencaps=yes" + RET);
                ipsec_conf.write(TAB + "xauth=server" + RET);
                ipsec_conf.write(TAB + "compress=yes" + RET);
                ipsec_conf.write(TAB + "auto=add" + RET);
                ipsec_conf.write(TAB + "rekey=yes" + RET);
                ipsec_conf.write(TAB + "ikelifetime=15m" + RET);
                ipsec_conf.write(TAB + "lifetime=15m" + RET);
                ipsec_conf.write(TAB + "left=" + listen.getAddress() + RET);
                ipsec_conf.write(TAB + "leftsubnet=0.0.0.0/0" + RET);
                ipsec_conf.write(TAB + "leftupdown=" + XAUTH_UPDOWN_SCRIPT + RET);
                ipsec_conf.write(TAB + "right=%any" + RET);
                ipsec_conf.write(TAB + "rightsourceip=" + settings.getVirtualXauthPool() + RET);

                // if no DNS servers are configured we use the server address of the L2TP interface
                if ((settings.getVirtualDnsOne().length() == 0) && (settings.getVirtualDnsTwo().length() == 0)) {
                    ipsec_conf.write(TAB + "rightdns=" + calculator.getFirstIP() + RET);
                }

                // handle only the first custom server
                if ((settings.getVirtualDnsOne().length() > 0) && (settings.getVirtualDnsTwo().length() == 0)) {
                    ipsec_conf.write(TAB + "rightdns=" + settings.getVirtualDnsOne() + RET);
                }

                // handle only the second custom server
                if ((settings.getVirtualDnsOne().length() == 0) && (settings.getVirtualDnsTwo().length() > 0)) {
                    ipsec_conf.write(TAB + "rightdns=" + settings.getVirtualDnsTwo() + RET);
                }

                // handle both the first and second custom server
                if ((settings.getVirtualDnsOne().length() > 0) && (settings.getVirtualDnsTwo().length() > 0)) {
                    ipsec_conf.write(TAB + "rightdns=" + settings.getVirtualDnsOne() + "," + settings.getVirtualDnsTwo() + RET);
                }

                ipsec_conf.write(RET);

                // -----------------------------------------------------------
                // create the IKEv2 config section for the interface
                // -----------------------------------------------------------

                ipsec_conf.write("conn VPN-IKEV2-" + Integer.toString(x) + RET);
                ipsec_conf.write(TAB + "keyexchange=ikev2" + RET);
                ipsec_conf.write(TAB + "auto=add" + RET);
                ipsec_conf.write(TAB + "type=tunnel" + RET);
                ipsec_conf.write(TAB + "left=" + listen.getAddress() + RET);

                if ((domainName == null) || (hostName == null)) {
                    ipsec_conf.write(TAB + "leftid=" + listen.getAddress() + RET);
                }

                else {
                    ipsec_conf.write(TAB + "leftid=" + hostName + "." + domainName + RET);
                }

                ipsec_conf.write(TAB + "leftauth=pubkey" + RET);
                ipsec_conf.write(TAB + "leftcert=" + ipsecCrtFile + RET);
                ipsec_conf.write(TAB + "leftsubnet=0.0.0.0/0" + RET);
                ipsec_conf.write(TAB + "leftsendcert=always" + RET);
                ipsec_conf.write(TAB + "leftupdown=" + IKEV2_UPDOWN_SCRIPT + RET);

                ipsec_conf.write(TAB + "right=%any" + RET);

                if (settings.getAuthenticationType() == IpsecVpnSettings.AuthenticationType.LOCAL_DIRECTORY) {
                    ipsec_conf.write(TAB + "rightauth=eap-mschapv2" + RET);
                }

                if (settings.getAuthenticationType() == IpsecVpnSettings.AuthenticationType.RADIUS_SERVER) {
                    ipsec_conf.write(TAB + "rightauth=eap-radius" + RET);
                }

                ipsec_conf.write(TAB + "rightsourceip=" + settings.getVirtualXauthPool() + RET);

                // if no DNS servers are configured we use the server address of the L2TP interface
                if ((settings.getVirtualDnsOne().length() == 0) && (settings.getVirtualDnsTwo().length() == 0)) {
                    ipsec_conf.write(TAB + "rightdns=" + calculator.getFirstIP() + RET);
                }

                // handle only the first custom server
                if ((settings.getVirtualDnsOne().length() > 0) && (settings.getVirtualDnsTwo().length() == 0)) {
                    ipsec_conf.write(TAB + "rightdns=" + settings.getVirtualDnsOne() + RET);
                }

                // handle only the second custom server
                if ((settings.getVirtualDnsOne().length() == 0) && (settings.getVirtualDnsTwo().length() > 0)) {
                    ipsec_conf.write(TAB + "rightdns=" + settings.getVirtualDnsTwo() + RET);
                }

                // handle both the first and second custom server
                if ((settings.getVirtualDnsOne().length() > 0) && (settings.getVirtualDnsTwo().length() > 0)) {
                    ipsec_conf.write(TAB + "rightdns=" + settings.getVirtualDnsOne() + "," + settings.getVirtualDnsTwo() + RET);
                }

                ipsec_conf.write(TAB + "rightsendcert=never" + RET);
                ipsec_conf.write(TAB + "eap_identity=%any" + RET);

                // add the L2TP PSK to the shared secrets file
                ipsec_secrets.write("# VPN-L2TP-" + Integer.toString(x) + RET);
                ipsec_secrets.write(listen.getAddress() + " %any : PSK 0x" + StringHexify(settings.getVirtualSecret()) + RET);
            }
        }

        boolean dnsWritten = false;

        // here we create the xl2tpd options file for the ppp daemon
        options_xl2tpd.write("lock" + RET);
        options_xl2tpd.write("auth" + RET);

        // The target file of the call statement gets created by directory connector and
        // sets the radius auth protocol (pap, chap, mschap, mschap-v2) that is configured.
        // We only include this if RADIUS is active otherwise the file may not exist.
        if (settings.getAuthenticationType() == IpsecVpnSettings.AuthenticationType.RADIUS_SERVER) {
            options_xl2tpd.write("call radius-auth-proto" + RET);
        }

        if (settings.getVirtualDnsOne().length() > 0) {
            options_xl2tpd.write("ms-dns " + settings.getVirtualDnsOne() + RET);
            dnsWritten = true;
        }

        if (settings.getVirtualDnsTwo().length() > 0) {
            options_xl2tpd.write("ms-dns " + settings.getVirtualDnsTwo() + RET);
            dnsWritten = true;
        }

        if (dnsWritten == false) {
            options_xl2tpd.write("ms-dns " + calculator.getFirstIP() + RET);
        }

        options_xl2tpd.write("lcp-echo-failure 30" + RET);
        options_xl2tpd.write("lcp-echo-interval 4" + RET);
        options_xl2tpd.write("ipparam L2TP" + RET);
        options_xl2tpd.write("mtu 1200" + RET);
        options_xl2tpd.write("mru 1200" + RET);
        options_xl2tpd.write("noproxyarp" + RET);
        options_xl2tpd.write("noktune" + RET);
        options_xl2tpd.write("noipv6" + RET);
        options_xl2tpd.write("nomppe" + RET);
        options_xl2tpd.write("local" + RET);

        if (settings.getDebugflag() == true) options_xl2tpd.write("debug" + RET);

        // if radius auth is selected all we need to do is add the two plugin
        // lines to the config since the dir con node takes care of the rest
        if (settings.getAuthenticationType() == IpsecVpnSettings.AuthenticationType.RADIUS_SERVER) {
            options_xl2tpd.write("plugin radius.so" + RET);
            options_xl2tpd.write("plugin radattr.so" + RET);
        }

        // now we create the xl2tpd.conf file
        xl2tpd_conf.write("[global]" + RET);
        xl2tpd_conf.write(TAB + "listen-addr = 0.0.0.0" + RET);
        xl2tpd_conf.write(TAB + "ipsec saref = no" + RET);
        xl2tpd_conf.write(TAB + "port = 1701" + RET);
        xl2tpd_conf.write(TAB + "debug network = " + (settings.getDebugflag() ? "yes" : "no") + RET);
        xl2tpd_conf.write(TAB + "debug packet = " + (settings.getDebugflag() ? "yes" : "no") + RET);
        xl2tpd_conf.write(TAB + "debug tunnel = " + (settings.getDebugflag() ? "yes" : "no") + RET);
        xl2tpd_conf.write(TAB + "debug state = " + (settings.getDebugflag() ? "yes" : "no") + RET);
        xl2tpd_conf.write(TAB + "debug avp = " + (settings.getDebugflag() ? "yes" : "no") + RET);
        xl2tpd_conf.write(RET);

        xl2tpd_conf.write("[lns default]" + RET);
        xl2tpd_conf.write(TAB + "ip range = " + calculator.getSecondIP() + " - " + calculator.getLastIP() + RET);
        xl2tpd_conf.write(TAB + "local ip = " + calculator.getFirstIP() + RET);
        xl2tpd_conf.write(TAB + "assign ip = yes" + RET);
        xl2tpd_conf.write(TAB + "require authentication = yes" + RET);
        xl2tpd_conf.write(TAB + "name = untangle-l2tp" + RET);
        xl2tpd_conf.write(TAB + "pppoptfile = /etc/ppp/options.xl2tpd" + RET);
        xl2tpd_conf.write(TAB + "length bit = yes" + RET);
        xl2tpd_conf.write(TAB + "ppp debug = " + (settings.getDebugflag() ? "yes" : "no") + RET);

        ipsec_secrets.write("# Include the xauth.secrets created by LocalDirectory" + RET);
        ipsec_secrets.write("include /etc/xauth.secrets" + RET);

        // here we create the strongswan.conf file which only includes strongswan.radius
        // that is created by directory connector if RADIUS auth is actually enabled
        strongswan_conf.write("charon {" + RET);
        strongswan_conf.write(TAB + "load_modular = yes" + RET);
        strongswan_conf.write(TAB + "plugins {" + RET);
        strongswan_conf.write(TAB + TAB + "include strongswan.d/charon/*.conf" + RET);

        if (settings.getAuthenticationType() == IpsecVpnSettings.AuthenticationType.RADIUS_SERVER) {
            strongswan_conf.write(TAB + TAB + "include /etc/strongswan.radius" + RET);
        }

        strongswan_conf.write(TAB + "}" + RET);
        strongswan_conf.write("}" + RET + RET);

        ipsec_conf.close();
        ipsec_secrets.close();
        options_xl2tpd.close();
        xl2tpd_conf.close();
        strongswan_conf.close();
    }

    private void writeIptablesScript(IpsecVpnSettings settings) throws Exception
    {
        logger.debug("writeIptablesScript(" + IPTABLES_GRE_SCRIPT + ")");

        AddressCalculator calculator = new AddressCalculator(settings.getVirtualNetworkPool());
        LinkedList<IpsecVpnNetwork> networkList = settings.getNetworks();
        String greAddr = calculator.getFirstIP();
        IpsecVpnNetwork network;
        String iface;
        String iaddr;
        int x, y;

        int httpsPort = UvmContextFactory.context().networkManager().getNetworkSettings().getHttpsPort();
        int httpPort = UvmContextFactory.context().networkManager().getNetworkSettings().getHttpPort();

        FileWriter gre_script = new FileWriter(IPTABLES_GRE_SCRIPT, false);

        gre_script.write("#!/bin/dash" + RET + "# " + IPTABLES_GRE_SCRIPT + RET + FILE_DISCLAIMER);

        gre_script.write("if [ -z \"$IPTABLES\" ] ; then IPTABLES=iptables ; fi" + RET + RET);

        gre_script.write("# delete all existing gre interfaces except 0 which is hidden and protected" + RET);
        gre_script.write("GRECOUNT=`cat /proc/net/dev | grep -v gre0 | grep gre | wc -l`" + RET);
        gre_script.write("for i in `seq 1 $GRECOUNT` ; do" + RET);
        gre_script.write(TAB + "${IPTABLES} -t mangle -D mark-src-intf -i gre$i -j MARK --set-mark 0xfd/0xff -m comment --comment \"Set src interface mark for GRE\" >/dev/null 2>&1" + RET);
        gre_script.write(TAB + "${IPTABLES} -t mangle -D mark-dst-intf -o gre$i -j MARK --set-mark 0xfd00/0xff00 -m comment --comment \"Set dst interface mark for GRE\" >/dev/null 2>&1" + RET);
        gre_script.write(TAB + "ip tunnel del gre$i" + RET);
        gre_script.write("done" + RET);
        gre_script.write(RET);

        gre_script.write("# delete all of the old WAN NAT rules" + RET);
        for (InterfaceSettings intfSettings : UvmContextFactory.context().networkManager().getNetworkSettings().getInterfaces()) {
            if (intfSettings.getConfigType() == InterfaceSettings.ConfigType.ADDRESSED && intfSettings.getIsWan()) {
                gre_script.write("${IPTABLES} -t nat -D nat-rules -m mark --mark 0x" + Integer.toHexString((intfSettings.getInterfaceId() << 8) + 0x00fd) + "/0xffff " + "-j MASQUERADE -m comment --comment \"NAT WAN-bound GRE traffic\" >/dev/null 2>&1" + RET);
            }
        }
        gre_script.write(RET);

        gre_script.write("# delete the old nat-reverse-filter rule" + RET);
        gre_script.write("${IPTABLES} -t filter -D nat-reverse-filter -m mark --mark 0xfd/0xff -j RETURN -m comment --comment \"Allow GRE\" >/dev/null 2>&1" + RET);
        gre_script.write(RET);

        gre_script.write("# delete the old admin forwards for GRE networks" + RET);
        gre_script.write("${IPTABLES} -t nat -D port-forward-rules -p tcp -d " + greAddr + " --destination-port " + httpsPort + " -j REDIRECT --to-ports 443 -m comment --comment \"Send GRE to apache\" >/dev/null 2>&1" + RET);
        gre_script.write("${IPTABLES} -t nat -D port-forward-rules -p tcp -d " + greAddr + " --destination-port " + httpPort + " -j REDIRECT --to-ports 80 -m comment --comment \"Send GRE to apache\" >/dev/null 2>&1" + RET);
        gre_script.write(RET);

        for (x = 0; x < networkList.size(); x++) {
            // For each active network we create a GRE interface
            // and add routes for the configured remote networks
            network = networkList.get(x);
            if (network.getActive() != true) continue;

            iface = ("gre" + String.valueOf(x + 1));
            iaddr = calculator.getOffsetIP(x + 1);

            gre_script.write("# IpsecVpnNetwork - " + network.getDescription() + RET);
            gre_script.write("ip tunnel add " + iface + " mode gre remote " + network.getRemoteAddress() + " local " + network.getLocalAddress() + " ttl 64" + RET);
            gre_script.write("ip link set " + iface + " up" + RET);
            gre_script.write("ip addr add " + iaddr + " dev " + iface + RET);

            String netlist[] = network.getRemoteNetworks().split("\\n");

            for (y = 0; y < netlist.length; y++) {
                gre_script.write("ip route add " + netlist[y] + " dev " + iface + RET);
            }

            gre_script.write("${IPTABLES} -t mangle -I mark-src-intf 4 -i " + iface + " -j MARK --set-mark 0xfd/0xff -m comment --comment \"Set src interface mark for GRE\"" + RET);
            gre_script.write("${IPTABLES} -t mangle -I mark-dst-intf 4 -o " + iface + " -j MARK --set-mark 0xfd00/0xff00 -m comment --comment \"Set dst interface mark for GRE\"" + RET);
            gre_script.write(RET);
        }

        gre_script.write("# create WAN NAT rules for each GRE interface" + RET);
        for (InterfaceSettings intfSettings : UvmContextFactory.context().networkManager().getNetworkSettings().getInterfaces()) {
            if (intfSettings.getConfigType() == InterfaceSettings.ConfigType.ADDRESSED && intfSettings.getIsWan()) {
                gre_script.write("${IPTABLES} -t nat -I nat-rules -m mark --mark 0x" + Integer.toHexString((intfSettings.getInterfaceId() << 8) + 0x00fd) + "/0xffff " + "-j MASQUERADE -m comment --comment \"NAT WAN-bound GRE traffic\"" + RET);
            }
        }
        gre_script.write(RET);

        gre_script.write("# create nat-reverse-filter rule to allow GRE to penetrate NATd networks" + RET);
        gre_script.write("${IPTABLES} -t filter -I nat-reverse-filter -m mark --mark 0xfd/0xff -j RETURN -m comment --comment \"Allow GRE\"" + RET);
        gre_script.write(RET);

        gre_script.write("# create admin forwards for GRE networks" + RET);
        gre_script.write("${IPTABLES} -t nat -I port-forward-rules -p tcp -d " + greAddr + " --destination-port " + httpsPort + " -j REDIRECT --to-ports 443 -m comment --comment \"Send GRE to apache\"" + RET);
        gre_script.write("${IPTABLES} -t nat -I port-forward-rules -p tcp -d " + greAddr + " --destination-port " + httpPort + " -j REDIRECT --to-ports 80 -m comment --comment \"Send GRE to apache\"" + RET);
        gre_script.write(RET);

        gre_script.close();

        UvmContextFactory.context().execManager().execResult("chmod 755 " + IPTABLES_GRE_SCRIPT);
    }

    private String StringHexify(String source)
    {
        // we convert the source to a hex string that can handle CR, LF, and
        // other characters and symbols that would otherwise break parsing
        // of the ipsec.secrets file
        StringBuilder secbuff = new StringBuilder();
        Formatter secform = new Formatter(secbuff);
        int val = 0;

        for (int l = 0; l < source.length(); l++) {
            // get the char as an integer and mask the sign bit
            // so we get character values between 0 and 255
            val = (source.charAt(l) & 0xff);
            secform.format("%02X", val);
        }

        return (secbuff.toString());
    }
}
