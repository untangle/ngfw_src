/**
 * $Id: AlertManagerImpl.java,v 1.00 2012/03/15 15:47:38 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.AlertManager;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.PolicyManager;
import com.untangle.uvm.networking.NetworkConfiguration;
import com.untangle.uvm.networking.ConnectionStatus;
import com.untangle.uvm.networking.InterfaceConfiguration;

/**
 * Implements AlertManager. This class runs a series of test and creates alerts
 * for important things the administrator should know about. The UI displays these
 * alerts when the admin logs into the UI.
 *
 * Possible future alerts to add:
 * recent high load?
 * frequent reboots?
 * semi-frequent power loss?
 * disk almost full?
 * modified sources.list?
 * high reports data retention time?
 * ping 8.8.8.8 for wan failover test?
 * change ifconfig to check percentage of errors
 * test RBL instead of checking 6 DNSs
 * have you messed with your casing settings
 */
class AlertManagerImpl implements AlertManager
{
    private final Logger logger = Logger.getLogger(this.getClass());

    private I18nUtil i18nUtil;
    
    public AlertManagerImpl()
    {
        Map<String,String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle-libuvm");
        this.i18nUtil = new I18nUtil(i18nMap);
    }
    
    public List<String> getAlerts()
    {
        LinkedList<String> alertList = new LinkedList<String>();

        try { testUpgrades(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testDNS(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testDupeApps(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testRendundantApps(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testBridgeBackwards(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testInterfaceErrors(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testSpamDNSServers(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }

        return alertList;
    }

    /**
     * This test tests to see if upgrades are available
     */
    private void testUpgrades(List<String> alertList)
    {
        try {
            if (UvmContextFactory.context().toolboxManager().getUpgradeStatus(false).getUpgradesAvailable()) {
                alertList.add(i18nUtil.tr("Upgrades are available and ready to be installed."));
            }
        } catch (Exception e) {}
    }
    
    /**
     * This test iterates through the DNS settings on each WAN and tests them individually
     * It creates an alert for each non-working DNS server
     */
    private void testDNS(List<String> alertList)
    {
        NetworkConfiguration networkConf = UvmContextFactory.context().networkManager().getNetworkConfiguration();
        ConnectivityTesterImpl connectivityTester = (ConnectivityTesterImpl)UvmContextFactory.context().getConnectivityTester();
        List<InetAddress> nonWorkingDns = new LinkedList<InetAddress>();
        
        for (InterfaceConfiguration intf : networkConf.getInterfaceList()) {
            if (!intf.isWAN())
                continue;
            
            InetAddress dnsPrimary   = intf.getDns1();
            InetAddress dnsSecondary = intf.getDns2();

            if (dnsPrimary != null)
                if (!connectivityTester.isDnsWorking(dnsPrimary, null))
                    nonWorkingDns.add(dnsPrimary);
            if (dnsSecondary != null)
                if (!connectivityTester.isDnsWorking(dnsSecondary, null))
                    nonWorkingDns.add(dnsSecondary);
        }

        if (nonWorkingDns.size() > 0) {
            String alertText = i18nUtil.tr("DNS connecitivity failed: ");
            for (InetAddress ia : nonWorkingDns) {
                alertText += ia.getHostAddress() + " ";
            }
            alertList.add(alertText);
        }
    }

    /**
     * This test for multiple instances of the same application in a given rack
     * This is never a good idea
     */
    private void testDupeApps(List<String> alertList)
    {
        LinkedList<NodeSettings> nodeSettingsList = UvmContextFactory.context().nodeManager().getSettings().getNodes();
        
        /**
         * Check each node for dupe nodes
         */
        for (NodeSettings n1 : nodeSettingsList ) {
            for (NodeSettings n2 : nodeSettingsList ) {
                if (n1.getId().equals(n2.getId()))
                    continue;

                /**
                 * If they have the same name and are in the same rack - they are dupes
                 * Check both for == and .equals so null is handled
                 */
                if (n1.getPolicyId() == n2.getPolicyId() && n1.getNodeName().equals(n2.getNodeName()))
                    alertList.add(i18nUtil.tr("A policy/rack") + " [" + n1.getPolicyId() + "] " + i18nUtil.tr("contains two or more") + " " + n1.getNodeName()); 
                if (n1.getPolicyId() == null || n2.getPolicyId() == null)
                    continue;
                if (n1.getPolicyId().equals(n2.getPolicyId()) && n1.getNodeName().equals(n2.getNodeName()))
                    alertList.add(i18nUtil.tr("A policy/rack") + " [" + n1.getPolicyId() + "] " + i18nUtil.tr("contains two or more") + " " + n1.getNodeName()); 
            }
        }
    }

    /**
     * This test iterates through each rack and test for redundant applications
     * It creates an alert for each redundant pair
     * Currently the redundant apps are:
     * Web Filter and Web Filter Lite
     * Spam Blocker and Spam Blocker Lite
     */
    private void testRendundantApps(List<String> alertList)
    {
        /**
         * Check for redundant apps 
         */
        List<Node> webfilterList = UvmContextFactory.context().nodeManager().nodeInstances("untangle-node-webfilter");
        List<Node> sitefilterList = UvmContextFactory.context().nodeManager().nodeInstances("untangle-node-sitefilter");
        List<Node> spamassassinList = UvmContextFactory.context().nodeManager().nodeInstances("untangle-node-spamassassin");
        List<Node> commtouchasList = UvmContextFactory.context().nodeManager().nodeInstances("untangle-node-commtouchas");

        for (Node node1 : webfilterList) {
            for (Node node2 : sitefilterList) {
                if (node1.getNodeSettings().getId().equals(node2.getNodeSettings().getId()))
                    continue;

                if (node1.getNodeSettings().getPolicyId().equals(node2.getNodeSettings().getPolicyId()))
                    alertList.add(i18nUtil.tr("One or more racks contain redundant apps") + ": " + " Web Filter " + i18nUtil.tr("and") + " Web Filter Lite" );
            }
        }

        for (Node node1 : spamassassinList) {
            for (Node node2 : commtouchasList) {
                if (node1.getNodeSettings().getId().equals(node2.getNodeSettings().getId()))
                    continue;

                if (node1.getNodeSettings().getPolicyId().equals(node2.getNodeSettings().getPolicyId()))
                    alertList.add(i18nUtil.tr("One or more racks contain redundant apps") + ": " + " Spam Blocker " + i18nUtil.tr("and") + " Spam Blocker Lite" );
            }
        }
    }

    /**
     * This test iterates through each bridged interface and tests that the bridge its in is not
     * "plugged in backwards"
     * It does this by checking the location of the bridge's gateway
     */
    private void testBridgeBackwards(List<String> alertList)
    {
        NetworkConfiguration networkConf = UvmContextFactory.context().networkManager().getNetworkConfiguration();
        
        for (InterfaceConfiguration intf : networkConf.getInterfaceList()) {
            if (!InterfaceConfiguration.CONFIG_BRIDGE.equals(intf.getConfigType()))
                continue;

            logger.debug("testBridgeBackwards: Checking Bridge: " + intf.getSystemName());
            logger.debug("testBridgeBackwards: Checking Bridge bridgedTo: " + intf.getBridgedTo());

            InterfaceConfiguration master = networkConf.findByName(intf.getBridgedTo());
            if (master == null) {
                logger.warn("Unable to locate bridge master: " + intf.getBridgedTo());
                continue;
            }
                
            logger.debug("testBridgeBackwards: Checking Bridge master: " + master.getSystemName());
            if (master.getSystemName() == null) {
                logger.warn("Unable to locate bridge master systemName: " + master.getName());
                continue;
            }
            String bridgeName = "br." + master.getSystemName();
            
            String result = UvmContextFactory.context().execManager().execOutput( "brctl showstp " + bridgeName + " | grep '^eth.*' | sed -e 's/(//g' -e 's/)//g'");
            if (result == null || "".equals(result)) {
                logger.warn("Unable to build bridge map");
                continue;
            }
            logger.debug("testBridgeBackwards: brctlOutput: " + result);

            /**
             * Parse output brctl showstp output. Example:
             * eth0 2
             * eth1 1
             */
            Map<Integer,String> bridgeIdToSystemNameMap = new HashMap<Integer,String>();
            for (String line : result.split("\n")) {
                logger.debug("testBridgeBackwards: line: " + line);
                String[] subline = line.split(" ");
                if (subline.length < 2) {
                    logger.warn("Invalid brctl showstp line: \"" + line + "\"");
                    break;
                }
                Integer key = Integer.parseInt(subline[1]);
                String systemName = subline[0];
                logger.debug("testBridgeBackwards: Map: " + key + " -> " + systemName);
                bridgeIdToSystemNameMap.put(key, systemName);
            }

            String gatewayIp = master.getGatewayStr();
            if (gatewayIp == null) {
                logger.warn("Missing gateway on bridge master");
                return;
            }

            /**
             * Lookup gateway MAC using arp -a
             */
            String gatewayMac = UvmContextFactory.context().execManager().execOutput( "arp -a " + gatewayIp + " | awk '{print $4}' ");
            if ( gatewayMac == null ) {
                logger.warn("Unable to determine MAC for " + gatewayIp);
                return;
            }
            gatewayMac = gatewayMac.replaceAll("\\s+","");
            if ( "".equals(gatewayMac) || "entries".equals(gatewayMac)) {
                logger.warn("Unable to determine MAC for " + gatewayIp);
                return;
            }
            
            /**
             * Lookup gateway bridge port # using brctl showmacs
             */
            String portNo = UvmContextFactory.context().execManager().execOutput( "brctl showmacs " + bridgeName + " | grep \"" + gatewayMac + "\" | awk '{print $1}'");
            if ( portNo == null) {
                logger.warn("Unable to port number for MAC" + gatewayMac);
                return;
            }
            portNo = portNo.replaceAll("\\s+","");
            if ( "".equals(portNo) ) {
                logger.warn("Unable to port number for MAC" + gatewayMac);
                return;
            }
            logger.debug("testBridgeBackwards: brctl showmacs Output: " + portNo);
            Integer gatewayPortNo = Integer.parseInt(portNo);
            logger.debug("testBridgeBackwards: Gateway Port: " + gatewayPortNo);


            /**
             * Lookup the system name for the bridge port
             */
            String gatewayInterfaceSystemName = bridgeIdToSystemNameMap.get(gatewayPortNo);
            logger.debug("testBridgeBackwards: Gateway Interface: " + gatewayInterfaceSystemName);
            if (gatewayInterfaceSystemName == null)  {
                logger.warn("Unable to find bridge port " + gatewayPortNo);
                return;
            }
            
            /**
             * Get the interface configuration for the interface where the gateway lives
             */
            InterfaceConfiguration gatewayIntf = networkConf.findBySystemName(gatewayInterfaceSystemName);
            if (gatewayIntf == null) {
                logger.warn("Unable to find gatewayIntf " + gatewayInterfaceSystemName);
                return;
            }
            logger.debug("testBridgeBackwards: Final Gateway Inteface: " + gatewayIntf.getName() + " is WAN: " + gatewayIntf.isWAN());

            /**
             * Ideally, this is the WAN, however if its actually an interface bridged to a WAN, then the interfaces are probably backwards
             */
            if (!gatewayIntf.isWAN()) {
                String alertText = i18nUtil.tr("Bridge");
                alertText += " (";
                alertText += master.getName();
                alertText += " <-> ";
                alertText += intf.getName();
                alertText += ") ";
                alertText += i18nUtil.tr("may be backwards.");
                alertText += " ";
                alertText += i18nUtil.tr("Gateway");
                alertText += " (";
                alertText += gatewayIp;
                alertText += ") ";
                alertText += i18nUtil.tr("is on ");
                alertText += " ";
                alertText += gatewayIntf.getName();
                alertText += ".";

                alertList.add(alertText);
            }
        }
    }

    /**
     * This test iterates through each interface and tests for
     * TX and RX errors on each interface.
     * It creates alert if there are a "high number" of errors
     */
    private void testInterfaceErrors(List<String> alertList)
    {
        NetworkConfiguration networkConf = UvmContextFactory.context().networkManager().getNetworkConfiguration();
        
        for (InterfaceConfiguration intf : networkConf.getInterfaceList()) {
            if (intf.getSystemName() == null || "tun0".equals(intf.getSystemName()))
                continue;
            String lines = UvmContextFactory.context().execManager().execOutput( "ifconfig " + intf.getSystemName() + " | grep errors | awk '{print $3}'");
            String type = "RX";  //first line is RX erros

            for (String line : lines.split("\n")) {
                line = line.replaceAll("\\s+","");
                logger.debug("testInterfaceErrors line: " + line);

                String[] errorsLine = line.split(":");
                if (errorsLine.length < 2)
                    continue;

                String errorsCountStr = errorsLine[1];
                Integer errorsCount;
                try { errorsCount = Integer.parseInt(errorsCountStr); } catch (NumberFormatException e) { continue; }

                /**
                 * Check for an arbitrarily high number of errors
                 * errors sometimes happen in small numbers and should be ignored
                 */
                if (errorsCount > 2500) {
                    String alertText = "";
                    alertText += intf.getName();
                    alertText += " ";
                    alertText += i18nUtil.tr("interface NIC card has a high number of");
                    alertText += " " + type + " ";
                    alertText += i18nUtil.tr("errors");
                    alertText += " (";
                    alertText += errorsCountStr;
                    alertText += ")";
                    alertText += ".";

                    alertList.add(alertText);
                }

                type = "TX"; // second line is TX
            }

        }
    }

    /**
     * This test tests to make sure public DNS servers are not used if spam blocking applications are installed
     */
    private void testSpamDNSServers(List<String> alertList)
    {
        List<Node> spamassassinList = UvmContextFactory.context().nodeManager().nodeInstances("untangle-node-spamassassin");
        List<Node> commtouchasList = UvmContextFactory.context().nodeManager().nodeInstances("untangle-node-commtouchas");

        if (spamassassinList.size() == 0 && commtouchasList.size() == 0)
            return;

        NetworkConfiguration networkConf = UvmContextFactory.context().networkManager().getNetworkConfiguration();
        
        for (InterfaceConfiguration intf : networkConf.getInterfaceList()) {
            if (!intf.isWAN())
                continue;
            
            String dnsPrimary   = (intf.getDns1() != null ? intf.getDns1().getHostAddress() : null);
            String dnsSecondary = (intf.getDns2() != null ? intf.getDns2().getHostAddress() : null);

            List<String> dnsServers = new LinkedList<String>();
            if ( dnsPrimary != null ) dnsServers.add(dnsPrimary);
            if ( dnsSecondary != null ) dnsServers.add(dnsSecondary);

            for (String dnsServer : dnsServers) {
                /* hardcode common known bad DNS */
                if ( "8.8.8.8".equals( dnsServer ) || /* google */
                     "8.8.4.4".equals( dnsServer ) || /* google */
                     "4.2.2.1".equals( dnsServer ) || /* level3 */
                     "4.2.2.2".equals( dnsServer ) || /* level3 */
                     "208.67.222.222".equals( dnsServer ) || /* openDNS */
                     "208.67.222.220".equals( dnsServer ) /* openDNS */ ) {
                    String alertText = "";
                    alertText += i18nUtil.tr("Spam Blocker [Lite] is installed but an unsupported DNS server is used");
                    alertText += " (";
                    alertText += intf.getName();
                    alertText += ",";
                    alertText += dnsServer;
                    alertText += ").";

                    alertList.add(alertText);
                }
                /* otherwise check each DNS against spamhaus */
                else {
                    int result = UvmContextFactory.context().execManager().execResult("host 2.0.0.127.zen.spamhaus.org " + dnsServer);
                    if (result != 0) {
                        String alertText = "";
                        alertText += i18nUtil.tr("Spam Blocker [Lite] is installed but a DNS server ");
                        alertText += " (";
                        alertText += intf.getName();
                        alertText += ",";
                        alertText += dnsServer;
                        alertText += ") fails to resolve DNSBL queries.";

                        alertList.add(alertText);
                    }
                }
            }
        }
    }
    
}