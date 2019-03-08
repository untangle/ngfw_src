/**
 * $Id: IpsecVpnScriptWriter.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.ipsec_vpn;

import java.util.LinkedList;
import java.io.FileWriter;

import org.apache.log4j.Logger;

import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.UvmContextFactory;

/**
 * This class has member functions to write the scripts that will be called by
 * untangle network stack to do all of the network configuration required by IPsec.
 * 
 * @author mahotz
 * 
 */

public class IpsecVpnScriptWriter
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String IPTABLES_IPSEC_SCRIPT = System.getProperty("prefix") + "/etc/untangle/iptables-rules.d/710-ipsec";
    private static final String IPTABLES_XAUTH_SCRIPT = System.getProperty("prefix") + "/etc/untangle/iptables-rules.d/711-xauth";
    private static final String IPTABLES_GRE_SCRIPT = System.getProperty("prefix") + "/etc/untangle/iptables-rules.d/712-gre";

    private static final String TAB = "\t";
    private static final String RET = "\n";

    /**
     * This function writes 710-ipsec which creates the rules required for IPsec
     * tunnel traffic and L2TP VPN traffic.
     * 
     * @param settings
     *        The IPsec settings
     * @throws Exception
     */
    protected void write_IPSEC_script(IpsecVpnSettings settings) throws Exception
    {
        logger.debug("write_IPSEC_script(" + IPTABLES_IPSEC_SCRIPT + ")");

        AddressCalculator calculator = new AddressCalculator(settings.getVirtualAddressPool());

        int httpsPort = UvmContextFactory.context().networkManager().getNetworkSettings().getHttpsPort();
        int httpPort = UvmContextFactory.context().networkManager().getNetworkSettings().getHttpPort();

        FileWriter script = null;
        try {
            script = new FileWriter(IPTABLES_IPSEC_SCRIPT, false);
            script.write("#!/bin/dash" + RET + RET + "# " + IPTABLES_IPSEC_SCRIPT + RET + IpsecVpnManager.FILE_DISCLAIMER);
            script.write("if [ -z \"$IPTABLES\" ] ; then IPTABLES=iptables ; fi" + RET + RET);

            script.write("# We put the L2TP port forward rules in their own chain that we can flush" + RET);
            script.write("# since the server address can be changed by the user meaning there is" + RET);
            script.write("# no easy way to find and delete any old rules" + RET);
            script.write("${IPTABLES} -t nat -N l2tp-forward-rules >/dev/null 2>&1" + RET);
            script.write("${IPTABLES} -t nat -F l2tp-forward-rules >/dev/null 2>&1" + RET + RET);

            script.write("# Allow IPsec traffic through the NAT reverse filter" + RET);
            script.write("${IPTABLES} -t filter -D nat-reverse-filter -m policy --pol ipsec --dir in  -j RETURN -m comment --comment \"allow IPsec traffic\" >/dev/null 2>&1" + RET);
            script.write("${IPTABLES} -t filter -D nat-reverse-filter -m policy --pol ipsec --dir out -j RETURN -m comment --comment \"allow IPsec traffic\" >/dev/null 2>&1" + RET);
            script.write("${IPTABLES} -t filter -I nat-reverse-filter -m policy --pol ipsec --dir in  -j RETURN -m comment --comment \"allow IPsec traffic\"" + RET);
            script.write("${IPTABLES} -t filter -I nat-reverse-filter -m policy --pol ipsec --dir out -j RETURN -m comment --comment \"allow IPsec traffic\"" + RET + RET);

            script.write("# Do not NAT ipsec traffic even if its leaving a WAN" + RET);
            script.write("${IPTABLES} -t nat -D POSTROUTING -m policy --pol ipsec --dir out -j RETURN -m comment --comment \"do not NAT IPsec traffic\" >/dev/null 2>&1" + RET);
            script.write("${IPTABLES} -t nat -I POSTROUTING -m policy --pol ipsec --dir out -j RETURN -m comment --comment \"do not NAT IPsec traffic\"" + RET + RET);

            script.write("# Remove any existing IPsec traffic bypass rules" + RET);
            script.write("${IPTABLES} -t filter -D bypass-rules -m policy --pol ipsec --dir out --goto set-bypass-mark >/dev/null 2>&1" + RET);
            script.write("${IPTABLES} -t filter -D bypass-rules -m policy --pol ipsec --dir in  --goto set-bypass-mark >/dev/null 2>&1" + RET + RET);

            if (settings.getBypassflag()) {
                script.write("# The bypass flag is set so add IPsec traffic bypass rules" + RET);
                script.write("${IPTABLES} -t filter -I bypass-rules -m policy --pol ipsec --dir out --goto set-bypass-mark" + RET);
                script.write("${IPTABLES} -t filter -I bypass-rules -m policy --pol ipsec --dir in  --goto set-bypass-mark" + RET + RET);
            }

            script.write("# NAT traffic from L2TP interfaces" + RET);
            script.write("${IPTABLES} -t nat -D nat-rules -m mark --mark 0xfb/0xff -j MASQUERADE -m comment --comment \"NAT l2tp traffic\" >/dev/null 2>&1" + RET);
            script.write("${IPTABLES} -t nat -I nat-rules -m mark --mark 0xfb/0xff -j MASQUERADE -m comment --comment \"NAT l2tp traffic\"" + RET + RET);

            script.write("# Allow L2TP traffic to penetrate NATd networks" + RET);
            script.write("${IPTABLES} -t filter -D nat-reverse-filter -m mark --mark 0xfb/0xff -j RETURN -m comment --comment \"Allow L2TP\" >/dev/null 2>&1" + RET);
            script.write("${IPTABLES} -t filter -I nat-reverse-filter -m mark --mark 0xfb/0xff -j RETURN -m comment --comment \"Allow L2TP\"" + RET + RET);

            script.write("# Add the jump rule for the L2TP port forwards" + RET);
            script.write("${IPTABLES} -t nat -D port-forward-rules -j l2tp-forward-rules -m comment --comment \"Port forward jump for L2TP\" >/dev/null 2>&1" + RET);
            script.write("${IPTABLES} -t nat -I port-forward-rules -j l2tp-forward-rules -m comment --comment \"Port forward jump for L2TP\"" + RET + RET);

            if (settings.getVpnflag()) {
                script.write("# Add port forward rules for L2TP and Xauth clients.  We need the port 53 rules" + RET);
                script.write("# for Xauth clients since the server side of the L2TP interface will not exist" + RET);
                script.write("# if no L2TP clients are connected.  Using the L2TP server side addresses for" + RET);
                script.write("# Xauth clients ended up being cleaner than trying to use the WAN interface." + RET);
                script.write("# We also don't need to do delete cleanup here since we're using dedicated" + RET);
                script.write("# chains that were flushed above." + RET);
                script.write("${IPTABLES} -t nat -I l2tp-forward-rules -p tcp -d " + calculator.getFirstIP() + " --destination-port " + httpsPort + " -j REDIRECT --to-ports 443 -m comment --comment \"Send L2TP to apache\"" + RET);
                script.write("${IPTABLES} -t nat -I l2tp-forward-rules -p tcp -d " + calculator.getFirstIP() + " --destination-port " + httpPort + " -j REDIRECT --to-ports 80 -m comment --comment \"Send L2TP to apache\"" + RET);
                script.write("${IPTABLES} -t nat -I l2tp-forward-rules -p tcp -d " + calculator.getFirstIP() + " --destination-port 53 -j REDIRECT --to-ports 53 -m comment --comment \"Send L2TP tcp to dnsmasq\"" + RET);
                script.write("${IPTABLES} -t nat -I l2tp-forward-rules -p udp -d " + calculator.getFirstIP() + " --destination-port 53 -j REDIRECT --to-ports 53 -m comment --comment \"Send L2TP udp to dnsmasq\"" + RET + RET);

                script.write("# This special rule blocks L2TP udp traffic on 1701 without IPsec" + RET);
                script.write("${IPTABLES} -t filter -D access-rules -p udp --dport 1701 -m policy --dir in --pol none -j DROP -m comment --comment \"drop L2TP without IPsec\" >/dev/null 2>&1" + RET);
                script.write("${IPTABLES} -t filter -I access-rules -p udp --dport 1701 -m policy --dir in --pol none -j DROP -m comment --comment \"drop L2TP without IPsec\"" + RET);
            }
        } catch( Exception ex ){
            logger.error("Unable to write script", ex);
        } finally{
            if( script != null){
                try {
                    script.close();
                } catch( Exception ex ){
                    logger.error("Unable close script", ex);
                }
            }
        }

        UvmContextFactory.context().execManager().execResult("chmod 755 " + IPTABLES_IPSEC_SCRIPT);
    }

    /**
     * This function writes 711-xauth which creates the rules required for Xauth
     * VPN traffic.
     * 
     * @param settings
     *        The IPsec settings
     * @throws Exception
     */
    protected void write_XAUTH_script(IpsecVpnSettings settings) throws Exception
    {
        logger.debug("write_XAUTH_script(" + IPTABLES_XAUTH_SCRIPT + ")");

        AddressCalculator calculator = new AddressCalculator(settings.getVirtualXauthPool());

        FileWriter script = null;
        try {
            script = new FileWriter(IPTABLES_XAUTH_SCRIPT, false);
            script.write("#!/bin/dash" + RET + RET + "# " + IPTABLES_XAUTH_SCRIPT + RET + IpsecVpnManager.FILE_DISCLAIMER);
            script.write("if [ -z \"$IPTABLES\" ] ; then IPTABLES=iptables ; fi" + RET + RET);

            script.write("# we put the rules to mark the ipsec xauth interface in their own chains since" + RET);
            script.write("# the cidr pool is used to identify the traffic and that could be changed" + RET);
            script.write("# by the user so we start by creating and flushing the chains" + RET);
            script.write("${IPTABLES} -t mangle -N ipsec-xauth-src >/dev/null 2>&1" + RET);
            script.write("${IPTABLES} -t mangle -N ipsec-xauth-dst >/dev/null 2>&1" + RET);
            script.write("${IPTABLES} -t mangle -F ipsec-xauth-src >/dev/null 2>&1" + RET);
            script.write("${IPTABLES} -t mangle -F ipsec-xauth-dst >/dev/null 2>&1" + RET + RET);

            script.write("# delete old jump rules (if they exist)" + RET);
            script.write("${IPTABLES} -t mangle -D mark-src-intf -j ipsec-xauth-src -m comment --comment \"src interface jump for Xauth\" >/dev/null 2>&1" + RET);
            script.write("${IPTABLES} -t mangle -D mark-dst-intf -j ipsec-xauth-dst -m comment --comment \"dst interface jump for Xauth\" >/dev/null 2>&1" + RET + RET);

            script.write("# delete old nat-rules rule" + RET);
            script.write("${IPTABLES} -t nat -D nat-rules -m mark --mark 0xfc/0xff -j MASQUERADE -m comment --comment \"NAT Xauth traffic\" >/dev/null 2>&1" + RET + RET);

            script.write("# delete old nat-reverse-filter rule" + RET);
            script.write("${IPTABLES} -t filter -D nat-reverse-filter -m mark --mark 0xfc/0xff -j RETURN -m comment --comment \"Allow Xauth\" >/dev/null 2>&1" + RET + RET);

            if (settings.getVpnflag()) {
                script.write("# first we add rules to the Xauth mark chains we prepared above" + RET);
                script.write("${IPTABLES} -t mangle -I ipsec-xauth-src -s " + settings.getVirtualXauthPool() + " -j MARK --set-mark 0xfc/0xff -m comment --comment \"Set src interface mark for Xauth\"" + RET);
                script.write("${IPTABLES} -t mangle -A ipsec-xauth-src -s " + settings.getVirtualXauthPool() + " -j CONNMARK --save-mark --mask 0xFFFF -m comment --comment \"Copy mark to connmark for Xauth\"" + RET);
                script.write("${IPTABLES} -t mangle -I ipsec-xauth-dst -d " + settings.getVirtualXauthPool() + " -j MARK --set-mark 0xfc00/0xff00 -m comment --comment \"Set dst interface mark for Xauth\"" + RET);
                script.write("${IPTABLES} -t mangle -A ipsec-xauth-dst -d " + settings.getVirtualXauthPool() + " -j CONNMARK --save-mark --mask 0xFFFF -m comment --comment \"Copy mark to connmark for Xauth\"" + RET + RET);

                script.write("# Add jump rules for xauth traffic.  These must be inserted ABOVE the line that" + RET);
                script.write("# returns if the marks are already set, otherwise the mark for the physical" + RET);
                script.write("# interface where the Xauth/IPsec traffic arrived will overwrite the special" + RET);
                script.write("# mark we using for our pseudo/virtual interface for Xauth traffic." + RET);
                script.write("${IPTABLES} -t mangle -I mark-src-intf -j ipsec-xauth-src -m comment --comment \"src interface jump for Xauth\"" + RET);
                script.write("${IPTABLES} -t mangle -I mark-dst-intf -j ipsec-xauth-dst -m comment --comment \"dst interface jump for Xauth\"" + RET + RET);

                script.write("# insert nat-reverse-filter rule to allow Xauth to penetrate NATd networks" + RET);
                script.write("${IPTABLES} -t filter -I nat-reverse-filter -m mark --mark 0xfc/0xff -j RETURN -m comment --comment \"Allow Xauth\"" + RET + RET);

                script.write("# insert nat-rules rule for traffic from xauth network" + RET);
                script.write("${IPTABLES} -t nat -I nat-rules -m mark --mark 0xfc/0xff -j MASQUERADE -m comment --comment \"NAT Xauth traffic\"" + RET);
            }
        } catch( Exception ex ){
            logger.error("Unable to write script", ex);
        } finally{
            if( script != null){
                try {
                    script.close();
                } catch( Exception ex ){
                    logger.error("Unable close script", ex);
                }
            }
        }

        UvmContextFactory.context().execManager().execResult("chmod 755 " + IPTABLES_XAUTH_SCRIPT);
    }

    /**
     * This function writes 712-gre which creates the interfaces and rules
     * required for GRE networks.
     * 
     * @param settings
     *        The IPsec settings
     * @throws Exception
     */
    protected void write_GRE_script(IpsecVpnSettings settings) throws Exception
    {
        logger.debug("write_GRE_script(" + IPTABLES_GRE_SCRIPT + ")");

        AddressCalculator calculator = new AddressCalculator(settings.getVirtualNetworkPool());
        LinkedList<IpsecVpnNetwork> networkList = settings.getNetworks();
        String greAddr = calculator.getFirstIP();
        IpsecVpnNetwork network;
        String iface;
        String iaddr;
        int x, y;

        int httpsPort = UvmContextFactory.context().networkManager().getNetworkSettings().getHttpsPort();
        int httpPort = UvmContextFactory.context().networkManager().getNetworkSettings().getHttpPort();

        FileWriter script = null;
        try{
            script = new FileWriter(IPTABLES_GRE_SCRIPT, false);
            script.write("#!/bin/dash" + RET + RET + "# " + IPTABLES_GRE_SCRIPT + RET + IpsecVpnManager.FILE_DISCLAIMER);
            script.write("if [ -z \"$IPTABLES\" ] ; then IPTABLES=iptables ; fi" + RET + RET);

            script.write("# delete all existing gre interfaces except 0 which is hidden and protected" + RET);
            script.write("GRECOUNT=`cat /proc/net/dev | grep -v gre0 | grep gre | wc -l`" + RET);
            script.write("for i in `seq 1 $GRECOUNT` ; do" + RET);
            script.write(TAB + "${IPTABLES} -t mangle -D mark-src-intf -i gre$i -j MARK --set-mark 0xfd/0xff -m comment --comment \"Set src interface mark for GRE\" >/dev/null 2>&1" + RET);
            script.write(TAB + "${IPTABLES} -t mangle -D mark-dst-intf -o gre$i -j MARK --set-mark 0xfd00/0xff00 -m comment --comment \"Set dst interface mark for GRE\" >/dev/null 2>&1" + RET);
            script.write(TAB + "ip tunnel del gre$i" + RET);
            script.write("done" + RET);
            script.write(RET);

            script.write("# delete all of the old WAN NAT rules" + RET);
            for (InterfaceSettings intfSettings : UvmContextFactory.context().networkManager().getNetworkSettings().getInterfaces()) {
                if (intfSettings.getConfigType() == InterfaceSettings.ConfigType.ADDRESSED && intfSettings.getIsWan()) {
                    script.write("${IPTABLES} -t nat -D nat-rules -m mark --mark 0x" + Integer.toHexString((intfSettings.getInterfaceId() << 8) + 0x00fd) + "/0xffff " + "-j MASQUERADE -m comment --comment \"NAT WAN-bound GRE traffic\" >/dev/null 2>&1" + RET);
                }
            }
            script.write(RET);

            script.write("# delete the old nat-reverse-filter rule" + RET);
            script.write("${IPTABLES} -t filter -D nat-reverse-filter -m mark --mark 0xfd/0xff -j RETURN -m comment --comment \"Allow GRE\" >/dev/null 2>&1" + RET);
            script.write(RET);

            script.write("# delete the old admin forwards for GRE networks" + RET);
            script.write("${IPTABLES} -t nat -D port-forward-rules -p tcp -d " + greAddr + " --destination-port " + httpsPort + " -j REDIRECT --to-ports 443 -m comment --comment \"Send GRE to apache\" >/dev/null 2>&1" + RET);
            script.write("${IPTABLES} -t nat -D port-forward-rules -p tcp -d " + greAddr + " --destination-port " + httpPort + " -j REDIRECT --to-ports 80 -m comment --comment \"Send GRE to apache\" >/dev/null 2>&1" + RET);
            script.write(RET);

            for (x = 0; x < networkList.size(); x++) {
                // For each active network we create a GRE interface
                // and add routes for the configured remote networks
                network = networkList.get(x);
                if (network.getActive() != true) continue;

                iface = ("gre" + String.valueOf(x + 1));
                iaddr = calculator.getOffsetIP(x + 1);

                script.write("# IpsecVpnNetwork - " + network.getDescription() + RET);
                script.write("ip tunnel add " + iface + " mode gre remote " + network.getRemoteAddress() + " local " + network.getLocalAddress() + " ttl " + Integer.toString(network.getTtl()) + RET);
                script.write("ip link set " + iface + " mtu " + Integer.toString(network.getMtu()) + RET);
                script.write("ip link set " + iface + " up" + RET);
                script.write("ip addr add " + iaddr + "/30 dev " + iface + RET);

                String netlist[] = network.getRemoteNetworks().split("\\n");

                for (y = 0; y < netlist.length; y++) {
                    if(netlist[y].trim().length() == 0){
                        continue;
                    }
                    script.write("ip route add " + netlist[y] + " dev " + iface + RET);
                }

                script.write("${IPTABLES} -t mangle -I mark-src-intf 4 -i " + iface + " -j MARK --set-mark 0xfd/0xff -m comment --comment \"Set src interface mark for GRE\"" + RET);
                script.write("${IPTABLES} -t mangle -I mark-dst-intf 4 -o " + iface + " -j MARK --set-mark 0xfd00/0xff00 -m comment --comment \"Set dst interface mark for GRE\"" + RET);
                script.write(RET);
            }

            script.write("# create WAN NAT rules for each GRE interface" + RET);
            for (InterfaceSettings intfSettings : UvmContextFactory.context().networkManager().getNetworkSettings().getInterfaces()) {
                if (intfSettings.getConfigType() == InterfaceSettings.ConfigType.ADDRESSED && intfSettings.getIsWan()) {
                    script.write("${IPTABLES} -t nat -I nat-rules -m mark --mark 0x" + Integer.toHexString((intfSettings.getInterfaceId() << 8) + 0x00fd) + "/0xffff " + "-j MASQUERADE -m comment --comment \"NAT WAN-bound GRE traffic\"" + RET);
                }
            }
            script.write(RET);

            script.write("# create nat-reverse-filter rule to allow GRE to penetrate NATd networks" + RET);
            script.write("${IPTABLES} -t filter -I nat-reverse-filter -m mark --mark 0xfd/0xff -j RETURN -m comment --comment \"Allow GRE\"" + RET);
            script.write(RET);

            script.write("# create admin forwards for GRE networks" + RET);
            script.write("${IPTABLES} -t nat -I port-forward-rules -p tcp -d " + greAddr + " --destination-port " + httpsPort + " -j REDIRECT --to-ports 443 -m comment --comment \"Send GRE to apache\"" + RET);
            script.write("${IPTABLES} -t nat -I port-forward-rules -p tcp -d " + greAddr + " --destination-port " + httpPort + " -j REDIRECT --to-ports 80 -m comment --comment \"Send GRE to apache\"" + RET);
            script.write(RET);
        } catch( Exception ex ){
            logger.error("Unable to write script", ex);
        } finally{
            if( script != null){
                try {
                    script.close();
                } catch( Exception ex ){
                    logger.error("Unable close script", ex);
                }
            }
        }

        UvmContextFactory.context().execManager().execResult("chmod 755 " + IPTABLES_GRE_SCRIPT);
    }
}
