/**
 * $Id: IpsecVpnManager.java 40784 2015-07-27 18:44:40Z mahotz $
 */

package com.untangle.app.ipsec_vpn;

import java.util.LinkedList;
import java.util.Formatter;
import java.io.FileWriter;

import org.apache.log4j.Logger;

import com.untangle.uvm.CertificateManager;
import com.untangle.uvm.UvmContextFactory;

/**
 * This class uses the application settings to dynamically generate the
 * configuration files used by the underlying IPsec implementation.
 * 
 * @author mahotz
 * 
 */

public class IpsecVpnManager
{
    private final Logger logger = Logger.getLogger(getClass());
    private final IpsecVpnScriptWriter scriptWriter = new IpsecVpnScriptWriter();

// THIS IS FOR ECLIPSE - @formatter:off

    private static final String TAB = "\t";
    private static final String RET = "\n";

    private static final String RELOAD_IPSEC_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-reload";
    private static final String XAUTH_UPDOWN_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-xauth-updown";
    private static final String IKEV2_UPDOWN_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-ikev2-updown";

    private static final String IPSEC_UNTANGLE_FILE = "/etc/ipsec.untangle";
    private static final String IPSEC_CONF_FILE = "/etc/ipsec.conf";
    private static final String IPSEC_SECRETS_FILE = "/etc/ipsec.secrets";
    private static final String OPTIONS_XL2TPD_FILE = "/etc/ppp/options.xl2tpd";
    private static final String XL2TPD_CONF_FILE = "/etc/xl2tpd/xl2tpd.conf";
    private static final String STRONGSWAN_CONF_FILE = "/etc/strongswan.conf";

    protected static final String FILE_DISCLAIMER = "# This file is created and maintained by the Untangle IPsec service." + RET +
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

// THIS IS FOR ECLIPSE - @formatter:on

    /**
     * This function will perform all actions required when new settings are
     * applied.
     * 
     * @param settings
     *        The application settings to be used
     * @param ipsecCertFile
     *        The certificate assigned for use by the IPsec server
     */
    public void generateConfig(IpsecVpnSettings settings, String ipsecCertFile)
    {
        logger.debug("generateConfig()");

        // generate all of the network scripts
        try {
            writeConfigFiles(settings, ipsecCertFile);
            scriptWriter.write_IPSEC_script(settings);
            scriptWriter.write_XAUTH_script(settings);
            scriptWriter.write_GRE_script(settings);
        }

        catch (Exception e) {
            logger.error("IpsecVpnManager.generateConfig()", e);
        }

        // call the ipsec reload script
        UvmContextFactory.context().execManager().exec(RELOAD_IPSEC_SCRIPT);

        /*
         * For every active tunnel configured to be always connected we call
         * ipsec route. This will install the kernel policy to automatically
         * bring the tunnel back up when traffic matching the tunnel policy is
         * detected and the tunnel is down for some reason.
         */
        LinkedList<IpsecVpnTunnel> tunnelList = settings.getTunnels();

        // call disconnectDSisabledTunnels to verify any disabled tunnels are now off
        disconnectDisabledTunnels(tunnelList);

        IpsecVpnTunnel tunnel;

        for (int x = 0; x < tunnelList.size(); x++) {
            tunnel = tunnelList.get(x);
            if (tunnel.getActive() != true) continue;
            if (tunnel.getRunmode().equals("start")) {
                UvmContextFactory.context().execManager().exec("ipsec route " + tunnel.getWorkName());
            }
        }
    }


    /**
     * disconnectDisabledTunnels will check inactive tunnels and bring them down if they are up
     * 
     * @param tunnelConfigs - LinkedList of IpsecVpnTunnel configurations 
     */
    public void disconnectDisabledTunnels(LinkedList<IpsecVpnTunnel> tunnelConfigs) {

        if(tunnelConfigs == null) return;

        try {
            for(IpsecVpnTunnel tun : tunnelConfigs) {
                if(!tun.getActive()) {
                    logger.info("disconnecting tunnel: " + tun.getWorkName());
                    UvmContextFactory.context().execManager().exec("ipsec down " + tun.getWorkName());
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to disconnect tunnel: ", e);
        }
    }

    /**
     * This function writes all of the low level IPsec configuration files using
     * the argumented settings.
     * 
     * @param settings
     *        The application settings to be used
     * @param ipsecCertFile
     *        The certificate assigned for use by the IPsec server
     * @throws Exception
     */
    private void writeConfigFiles(IpsecVpnSettings settings, String ipsecCertFile) throws Exception
    {
        logger.debug("writeConfigFiles()");

        String ipsecCrtFile = CertificateManager.CERT_STORE_PATH + ipsecCertFile.replaceAll("\\.pem", "\\.crt");
        String ipsecKeyFile = CertificateManager.CERT_STORE_PATH + ipsecCertFile.replaceAll("\\.pem", "\\.key");
        String domainName = UvmContextFactory.context().networkManager().getNetworkSettings().getDomainName();
        String hostName = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName();

        LinkedList<IpsecVpnTunnel> tunnelList = settings.getTunnels();
        LinkedList<VirtualListen> listenList = settings.getVirtualListenList();
        VirtualListen listen;
        IpsecVpnTunnel data;
        int x;

        /*
         * Some customers want manual control of the ipsec.conf file to set low
         * level features and options that we don't currently support in the
         * user interface. To support them we added a special neverWriteconfig
         * boolean that can be manually enabled in the settings file. When set,
         * we write our config to ipsec.untangle instead of ipsec.conf, leaving
         * manual modifications unchanged.
         */
        FileWriter ipsec_conf = null;
        FileWriter ipsec_secrets = null;
        FileWriter options_xl2tpd = null;
        FileWriter xl2tpd_conf = null;
        FileWriter strongswan_conf = null;

        try{

            if (settings.getNeverWriteConfig() == true) {
                ipsec_conf = new FileWriter(IPSEC_UNTANGLE_FILE, false);
            } else {
                ipsec_conf = new FileWriter(IPSEC_CONF_FILE, false);
            }

            ipsec_secrets = new FileWriter(IPSEC_SECRETS_FILE, false);
            options_xl2tpd = new FileWriter(OPTIONS_XL2TPD_FILE, false);
            xl2tpd_conf = new FileWriter(XL2TPD_CONF_FILE, false);
            strongswan_conf = new FileWriter(STRONGSWAN_CONF_FILE, false);

            AddressCalculator calculator = new AddressCalculator(settings.getVirtualAddressPool());

            /*
             * When running on ARM we have to disable the TCP replay protection,
             * otherwise we see massive packet loss. The tunnel is up but when
             * testing with ping only 1 in 1000 or so replies are actually received
             * properly. Using tcpdump we see all the replies and everything looks
             * correct, but almost all seem to be ignored by charon. Our best guess
             * is the unique switch/network setup on this device is somehow
             * triggering the replay detection, so our current solution is adding
             * this config option.
             */

            String osArch = System.getProperty("os.arch", "unknown");

            ipsec_conf.write("# " + IPSEC_CONF_FILE + RET + FILE_DISCLAIMER);
            ipsec_secrets.write("# " + IPSEC_SECRETS_FILE + RET + FILE_DISCLAIMER);
            options_xl2tpd.write("# " + OPTIONS_XL2TPD_FILE + RET + FILE_DISCLAIMER);
            xl2tpd_conf.write("; " + XL2TPD_CONF_FILE + RET + FILE_DISCLAIMER.replaceAll("#", ";"));
            strongswan_conf.write("# " + STRONGSWAN_CONF_FILE + RET + FILE_DISCLAIMER);

            if(settings != null){
                // first we create the setup section in the ipsec.conf file
                // this section is required and contains general protocol
                // config directives and items that are common to all tunnels
                ipsec_conf.write("config setup" + RET);
                ipsec_conf.write(TAB + "uniqueids=" + settings.getUniqueIds() + RET);

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

                    ipsec_conf.write("conn UT" + Integer.toString(data.getId()) + "_" + workname + RET);
                    ipsec_conf.write(TAB + "keyexchange=ikev" + Integer.toString(data.getIkeVersion()) + RET);
                    ipsec_conf.write(TAB + "type=" + data.getConntype() + RET);
                    ipsec_conf.write(TAB + "authby=psk" + RET);

                    ipsec_conf.write(TAB + "rekey=yes" + RET);
                    ipsec_conf.write(TAB + "keyingtries=%forever" + RET);

                    if (osArch.equals("arm") == true) {
                        ipsec_conf.write(TAB + "replay_window=0" + RET);
                    }

                    if (data.getPhase1Manual() == true) {
                        ipsec_conf.write(TAB + "ike=" + data.getPhase1Cipher() + "-" + data.getPhase1Hash() + "-" + data.getPhase1Group() + "!" + RET);
                        ipsec_conf.write(TAB + "ikelifetime=" + data.getPhase1Lifetime() + "s" + RET);
                    } else {
                        ipsec_conf.write(TAB + "ike=" + ike_default + RET);
                        ipsec_conf.write(TAB + "ikelifetime=" + settings.getPhase1DefaultLifetime() + RET);
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
                        ipsec_conf.write(TAB + "lifetime=" + settings.getPhase2DefaultLifetime() + RET);
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

                    ipsec_conf.write(TAB + "leftsubnet=" + data.getLeftSubnet() + RET);
                    ipsec_conf.write(TAB + "right=" + data.getRight() + RET);

                    // use the configured rightid if available otherwise use right
                    if ((data.getRightId() != null) && (data.getRightId().length() > 0)) {
                        ipsec_conf.write(TAB + "rightid=" + data.getRightId() + RET);
                    } else {
                        ipsec_conf.write(TAB + "rightid=" + data.getRight() + RET);
                    }

                    ipsec_conf.write(TAB + "rightsubnet=" + data.getRightSubnet() + RET);
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

                        if (osArch.equals("arm") == true) {
                            ipsec_conf.write(TAB + "replay_window=0" + RET);
                        }

                        ipsec_conf.write(TAB + "ikelifetime=" + settings.getPhase1DefaultLifetime() + RET);
                        ipsec_conf.write(TAB + "lifetime=" + settings.getPhase2DefaultLifetime() + RET);
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

                        if (osArch.equals("arm") == true) {
                            ipsec_conf.write(TAB + "replay_window=0" + RET);
                        }

                        ipsec_conf.write(TAB + "ikelifetime=" + settings.getPhase1DefaultLifetime() + RET);
                        ipsec_conf.write(TAB + "lifetime=" + settings.getPhase2DefaultLifetime() + RET);
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
                        ipsec_conf.write(TAB + "ikelifetime=" + settings.getPhase1DefaultLifetime() + RET);
                        ipsec_conf.write(TAB + "lifetime=" + settings.getPhase2DefaultLifetime() + RET);

                        if (osArch.equals("arm") == true) {
                            ipsec_conf.write(TAB + "replay_window=0" + RET);
                        }

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
                // lines to the config since the dir con app takes care of the rest
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
            }
        } catch( Exception e){
            logger.error("Unable to write file", e);
        } finally {
            if( ipsec_conf != null ){
                try {
                    ipsec_conf.close();
                } catch( Exception e ){
                    logger.error("Unable to close file", e);
                }
            }
            if( ipsec_secrets != null ){
                try {
                    ipsec_secrets.close();
                } catch( Exception e ){
                    logger.error("Unable to close file", e);
                }
            }
            if( options_xl2tpd != null ){
                try {
                    options_xl2tpd.close();
                } catch( Exception e ){
                    logger.error("Unable to close file", e);
                }
            }
            if( xl2tpd_conf != null ){
                try {
                    xl2tpd_conf.close();
                } catch( Exception e ){
                    logger.error("Unable to close file", e);
                }
            }
            if( strongswan_conf != null ){
                try {
                    strongswan_conf.close();
                } catch( Exception e ){
                    logger.error("Unable to close file", e);
                }
            }
        }

        // if (settings == null) {
        //     ipsec_conf.close();
        //     ipsec_secrets.close();
        //     options_xl2tpd.close();
        //     xl2tpd_conf.close();
        //     return;
        // }


        // ipsec_conf.close();
        // ipsec_secrets.close();
        // options_xl2tpd.close();
        // xl2tpd_conf.close();
        // strongswan_conf.close();
    }

    /**
     * This function takes a normal string and converts it to a string of hex
     * digit pairs representing each character in the original string. We use it
     * to obfuscate passwords in the ipsec.secrets file. Using the hex format
     * also prevents problems with embeding characters that may also be a
     * parsing token used by one of the IPsec applications or daemons.
     * 
     * @param source
     *        The string to convert to hex format
     * 
     * @return The hex format string
     */
    private String StringHexify(String source)
    {
        // we convert the source to a hex string that can handle CR, LF, and
        // other characters and symbols that would otherwise break parsing
        // of the ipsec.secrets file
        StringBuilder secbuff = new StringBuilder();

        Formatter secform = null;

        try{ 
            secform = new Formatter(secbuff);
            int val = 0;

            for (int l = 0; l < source.length(); l++) {
                // get the char as an integer and mask the sign bit
                // so we get character values between 0 and 255
                val = (source.charAt(l) & 0xff);
                secform.format("%02X", val);
            }
        } catch( Exception ex ){
            logger.error("Unable to format ", ex );
        } finally {
            if (secform != null){
                try {
                    secform.close();
                } catch( Exception ex ){
                    logger.error("Unable to close formatter ", ex );
                }
            }
        }

        return (secbuff.toString());
    }
}
