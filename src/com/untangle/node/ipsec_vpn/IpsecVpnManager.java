/**
 * $Id: IpsecVpnManager.java 40784 2015-07-27 18:44:40Z mahotz $
 */

package com.untangle.node.ipsec_vpn;

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

import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.UvmContextFactory;

public class IpsecVpnManager
{
    private final Logger logger = Logger.getLogger(getClass());
    private final String TAB = "\t";
    private final String RET = "\n";

// THIS IS FOR ECLIPSE - @formatter:off

    private final String RELOAD_IPSEC_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-reload";
    private final String XAUTH_UPDOWN_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-xauth-updown";

    private final String FILE_DISCLAIMER =  "# This file is created and maintained by the Untangle IPsec service." + RET +
                                            "# If you modify this file manually, your changes will be overwritten!" + RET + RET;

    // these are the default values that will be used when phase 1/2 manual configuration is NOT enabled
    // and were chosen for maximum compatibility with our previous releases that used openswan 

    private final String ike_default = "3des-md5-modp2048,3des-md5-modp1536,3des-md5-modp1024," +
                                       "3des-sha1-modp2048,3des-sha1-modp1536,3des-sha1-modp1024," +
                                       "aes128-md5-modp2048,aes128-md5-modp1536,aes128-md5-modp1024," +
                                       "aes128-sha1-modp2048,aes128-sha1-modp1536,aes128-sha1-modp1024," +
                                       "aes256-md5-modp2048,aes256-md5-modp1536,aes256-md5-modp1024," +
                                       "aes256-sha1-modp2048,aes256-sha1-modp1536,aes256-sha1-modp1024";

    private final String esp_default = "3des-md5-modp2048,3des-md5-modp1536,3des-md5-modp1024," +
                                       "3des-sha1-modp2048,3des-sha1-modp1536,3des-sha1-modp1024," +
                                       "aes128-md5-modp2048,aes128-md5-modp1536,aes128-md5-modp1024," +
                                       "aes128-sha1-modp2048,aes128-sha1-modp1536,aes128-sha1-modp1024," +
                                       "aes256-md5-modp2048,aes256-md5-modp1536,aes256-md5-modp1024," +
                                       "aes256-sha1-modp2048,aes256-sha1-modp1536,aes256-sha1-modp1024";
    
    private final String ikelifetime_default = "1h";
    private final String lifetime_default = "8h";

// THIS IS FOR ECLIPSE - @formatter:on

    public void generateConfig(IpsecVpnSettings settings)
    {
        logger.debug("generateConfig()");

        try {
            writeConfigFiles(settings);
            UvmContextFactory.context().execManager().exec(RELOAD_IPSEC_SCRIPT);
        }

        catch (Exception e) {
            logger.error("IpsecVpnManager.generateConfig()", e);
        }
    }

    private void writeConfigFiles(IpsecVpnSettings settings) throws Exception
    {
        logger.debug("writeConfigFiles()");

        LinkedList<IpsecVpnTunnel> tunnelList = settings.getTunnels();
        LinkedList<VirtualListen> listenList = settings.getVirtualListenList();
        VirtualListen listen;
        IpsecVpnTunnel data;
        int x;

        FileWriter ipsec_conf = new FileWriter("/etc/ipsec.conf", false);
        FileWriter ipsec_secrets = new FileWriter("/etc/ipsec.secrets", false);
        FileWriter options_xl2tpd = new FileWriter("/etc/ppp/options.xl2tpd", false);
        FileWriter xl2tpd_conf = new FileWriter("/etc/xl2tpd/xl2tpd.conf", false);
        FileWriter strongswan_conf = new FileWriter("/etc/strongswan.conf", false);

        AddressCalculator calculator = new AddressCalculator(settings.getVirtualAddressPool());

        ipsec_conf.write("# /etc/ipsec.conf" + RET + FILE_DISCLAIMER);
        ipsec_secrets.write("# /etc/ipsec.secrets" + RET + FILE_DISCLAIMER);
        options_xl2tpd.write("# /etc/ppp/options.xl2tpd" + RET + FILE_DISCLAIMER);
        xl2tpd_conf.write("; /etc/xl2tpd/xl2tpd.conf" + RET + FILE_DISCLAIMER.replaceAll("#", ";"));
        strongswan_conf.write("# /etc/strongswan.conf" + RET + FILE_DISCLAIMER);

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
            ipsec_conf.write(TAB + "keyexchange=ikev1" + RET);
            ipsec_conf.write(TAB + "type=" + data.getConntype() + RET);
            ipsec_conf.write(TAB + "authby=psk" + RET);

            ipsec_conf.write(TAB + "rekey=yes" + RET);
            ipsec_conf.write(TAB + "keyingtries=%forever" + RET);

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
            ipsec_conf.write(TAB + "leftsubnet=" + leftString + RET);
            ipsec_conf.write(TAB + "right=" + data.getRight() + RET);
            ipsec_conf.write(TAB + "rightsubnet=" + rightString + RET);
            ipsec_conf.write(TAB + "rightid=%any" + RET);
            ipsec_conf.write(TAB + "auto=" + data.getRunmode() + RET);
            ipsec_conf.write(RET);

            // add the tunnel PSK to the ipsec.secrets file
            ipsec_secrets.write("# " + workname + RET);
            ipsec_secrets.write(data.getLeft() + " " + data.getRight() + " : PSK 0x" + StringHexify(data.getSecret()) + RET);
        }

        // if the L2TP/Xauth server is enabled then we create a config section
        // section for each one on each configured listen address
        if (settings.getVpnflag() == true) {
            for (x = 0; x < listenList.size(); x++) {
                listen = listenList.get(x);

                // create the L2TP config section for the interface
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

                // create the Xauth config section for the interface
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
