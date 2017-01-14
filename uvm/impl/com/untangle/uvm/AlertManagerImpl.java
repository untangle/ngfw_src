/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.LinkedList;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.net.InetAddress;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.File;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.AlertManager;
import com.untangle.uvm.ExecManager;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.PolicyManager;
import com.untangle.uvm.node.Reporting;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.StaticRoute;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Type;
import org.xbill.DNS.SimpleResolver;

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
 */
public class AlertManagerImpl implements AlertManager
{
    private final Logger logger = Logger.getLogger(this.getClass());

    private I18nUtil i18nUtil;

    private ExecManager execManager = null;

    public AlertManagerImpl()
    {
        Map<String,String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle");
        this.i18nUtil = new I18nUtil(i18nMap);
    }

    public synchronized List<String> getAlerts()
    {
        LinkedList<String> alertList = new LinkedList<String>();
        boolean dnsWorking = false;

        this.execManager = UvmContextFactory.context().createExecManager();

        try { testUpgrades(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { dnsWorking = testDNS(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { if (dnsWorking) testConnectivity(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { if (dnsWorking) testConnector(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testDiskFree(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testDiskErrors(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testDupeApps(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testRendundantApps(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testBridgeBackwards(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testInterfaceErrors(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testSpamDNSServers(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testZveloDNSServers(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testEventWriteTime(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testEventWriteDelay(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testShieldEnabled(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testRoutesToReachableAddresses(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testServerConf(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }
        try { testLicenseCompliance(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }

        /**
         * Disabled Tests
         */
        //try { testQueueFullMessages(alertList); } catch (Exception e) { logger.warn("Alert test exception",e); }

        this.execManager.close();
        this.execManager = null;

        return alertList;
    }

    /**
     * This test tests to see if upgrades are available
     */
    private void testUpgrades(List<String> alertList)
    {
        try {
            if (UvmContextFactory.context().systemManager().upgradesAvailable(false)) {
                alertList.add(i18nUtil.tr("Upgrades are available and ready to be installed."));
            }
        } catch (Exception e) {}
    }

    /**
     * This test iterates through the DNS settings on each WAN and tests them individually
     * It creates an alert for each non-working DNS server
     */
    private boolean testDNS(List<String> alertList)
    {
        ConnectivityTesterImpl connectivityTester = (ConnectivityTesterImpl)UvmContextFactory.context().getConnectivityTester();
        List<InetAddress> nonWorkingDns = new LinkedList<InetAddress>();

        for ( InterfaceSettings intf : UvmContextFactory.context().networkManager().getEnabledInterfaces() ) {
            if (!intf.getIsWan())
                continue;

            InetAddress dnsPrimary   = UvmContextFactory.context().networkManager().getInterfaceStatus( intf.getInterfaceId() ).getV4Dns1();
            InetAddress dnsSecondary = UvmContextFactory.context().networkManager().getInterfaceStatus( intf.getInterfaceId() ).getV4Dns2();

            if (dnsPrimary != null)
                if (!connectivityTester.isDnsWorking(dnsPrimary, null))
                    nonWorkingDns.add(dnsPrimary);
            if (dnsSecondary != null)
                if (!connectivityTester.isDnsWorking(dnsSecondary, null))
                    nonWorkingDns.add(dnsSecondary);
        }

        if (nonWorkingDns.size() > 0) {
            String alertText = i18nUtil.tr("DNS connectivity failed:") + " ";
            for (InetAddress ia : nonWorkingDns) {
                alertText += ia.getHostAddress() + " ";
            }
            alertList.add(alertText);
            return false;
        }

        return true;
    }

    /**
     * This test tests connectivity to key servers in the untangle datacenter
     */
    private void testConnectivity(List<String> alertList)
    {
        Socket socket = null;

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("updates.untangle.com",80), 7000);
        } catch ( Exception e ) {
            alertList.add( i18nUtil.tr("Failed to connect to Untangle." +  " [updates.untangle.com:80]") );
        } finally {
            try {if (socket != null) socket.close();} catch (Exception e) {}
        }

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("license.untangle.com",443), 7000);
        } catch ( Exception e ) {
            alertList.add( i18nUtil.tr("Failed to connect to Untangle." +  " [license.untangle.com:443]") );
        } finally {
            try {if (socket != null) socket.close();} catch (Exception e) {}
        }
    }

    /**
     * This test that pyconnector is connected to cmd.untangle.com
     */
    private void testConnector(List<String> alertList)
    {
        try {
            if ( UvmContextFactory.context().isDevel() )
                return;
            if ( ! UvmContextFactory.context().systemManager().getSettings().getCloudEnabled() )
                return;

            File pidFile = new File("/var/run/ut-pyconnector.pid");
            if ( !pidFile.exists() ) {
                alertList.add( i18nUtil.tr("Failed to connect to Untangle." +  " [cmd.untangle.com]") );
                return;
            }

            int result = this.execManager.execResult(System.getProperty("uvm.bin.dir") + "/ut-pyconnector-status");
            if (result != 0)
                alertList.add( i18nUtil.tr("Failed to connect to Untangle." +  " [cmd.untangle.com]") );
        } catch (Exception e) {

        }
    }

    /*
     * This test that disk free % is less than 75%
     */
    private void testDiskFree(List<String> alertList)
    {
        String result = this.execManager.execOutput( "df -k / | awk '/\\//{printf(\"%d\",$5)}'");

        try {
            int percentUsed = Integer.parseInt(result);
            if (percentUsed > 75)
                alertList.add( i18nUtil.tr("Free Disk space is low.") +  " [ " + (100 - percentUsed) + i18nUtil.tr("% free ]") );
        } catch (Exception e) {
            logger.warn("Unable to determine free disk space",e);
        }

    }

    /**
     * Looks for somewhat comman errors in kern.log related to problematic disks
     */
    private void testDiskErrors(List<String> alertList)
    {
        ExecManagerResult result;

        result = this.execManager.exec( "tail -n 15000 /var/log/kern.log | grep -m1 -B3 'DRDY ERR'" );
        if ( result.getResult() == 0 ) {
            alertList.add( i18nUtil.tr("Disk errors reported.") + "<br/>\n" + result.getOutput().replaceAll("\n","<br/>\n") );
        }

        result = this.execManager.exec( "tail -n 15000 /var/log/kern.log | grep -m1 -B3 'I/O error'" );
        if ( result.getResult() == 0 ) {
            alertList.add( i18nUtil.tr("Disk errors reported.") + "<br/>\n" + result.getOutput().replaceAll("\n","<br/>\n") );
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
                if (n1.getPolicyId() == null || n2.getPolicyId() == null) {
                    if (n1.getPolicyId() == n2.getPolicyId() && n1.getNodeName().equals(n2.getNodeName()))
                        alertList.add( i18nUtil.tr("Services contains two or more") + " " + n1.getNodeName() );
                } else {
                    if (n1.getPolicyId().equals(n2.getPolicyId()) && n1.getNodeName().equals(n2.getNodeName()))
                        alertList.add(i18nUtil.tr("A policy/rack") + " [" + n1.getPolicyId() + "] " + i18nUtil.tr("contains two or more") + " " + n1.getNodeName());
                }
            }
        }
    }

    /**
     * This test iterates through each rack and test for redundant applications
     * It creates an alert for each redundant pair
     * Currently the redundant apps are:
     * Web Filter and Web Filter Lite
     * Spam Blocker and Spam Blocker Lite
     * Web Filter and Web Monitor
     */
    private void testRendundantApps(List<String> alertList)
    {
        /**
         * Check for redundant apps
         */
        List<Node> webFilterLiteList = UvmContextFactory.context().nodeManager().nodeInstances("untangle-node-web-filter-lite");
        List<Node> webFilterList = UvmContextFactory.context().nodeManager().nodeInstances("untangle-node-web-filter");
        List<Node> spamBlockerLiteList = UvmContextFactory.context().nodeManager().nodeInstances("untangle-node-spam-blocker-lite");
        List<Node> spamblockerList = UvmContextFactory.context().nodeManager().nodeInstances("untangle-node-spamblocker");
        List<Node> webMonitorList = UvmContextFactory.context().nodeManager().nodeInstances("untangle-node-web-monitor");

        for (Node node1 : webFilterLiteList) {
            for (Node node2 : webFilterList) {
                if (node1.getNodeSettings().getId().equals(node2.getNodeSettings().getId()))
                    continue;

                if (node1.getNodeSettings().getPolicyId().equals(node2.getNodeSettings().getPolicyId()))
                    alertList.add(i18nUtil.tr("One or more racks contain redundant apps") + ": " + " Web Filter " + i18nUtil.tr("and") + " Web Filter Lite" );
            }
        }

        for (Node node1 : webMonitorList) {
            for (Node node2 : webFilterList) {
                if (node1.getNodeSettings().getId().equals(node2.getNodeSettings().getId()))
                    continue;

                if (node1.getNodeSettings().getPolicyId().equals(node2.getNodeSettings().getPolicyId()))
                    alertList.add(i18nUtil.tr("One or more racks contain redundant apps") + ": " + " Web Filter " + i18nUtil.tr("and") + " Web Monitor" );
            }
        }

        for (Node node1 : spamBlockerLiteList) {
            for (Node node2 : spamblockerList) {
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
        for ( InterfaceSettings intf : UvmContextFactory.context().networkManager().getEnabledInterfaces() ) {
            if (!InterfaceSettings.ConfigType.BRIDGED.equals( intf.getConfigType() ))
                continue;

            logger.debug("testBridgeBackwards: Checking Bridge: " + intf.getSystemDev());
            logger.debug("testBridgeBackwards: Checking Bridge bridgedTo: " + intf.getBridgedTo());

            InterfaceSettings master = UvmContextFactory.context().networkManager().findInterfaceId(intf.getBridgedTo());
            if (master == null) {
                logger.warn("Unable to locate bridge master: " + intf.getBridgedTo());
                continue;
            }

            logger.debug("testBridgeBackwards: Checking Bridge master: " + master.getSystemDev());
            if (master.getSystemDev() == null) {
                logger.warn("Unable to locate bridge master systemName: " + master.getName());
                continue;
            }
            String bridgeName = master.getSymbolicDev();

            String result = this.execManager.execOutput( "brctl showstp " + bridgeName + " | grep '^eth.*' | sed -e 's/(//g' -e 's/)//g'");
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
                Integer key;
                try {
                    key = Integer.parseInt(subline[1]);
                } catch (Exception e) {
                    logger.warn("Invalid output: " + subline[1]);
                    continue;
                }
                String systemName = subline[0];
                logger.debug("testBridgeBackwards: Map: " + key + " -> " + systemName);
                bridgeIdToSystemNameMap.put(key, systemName);
            }

            /**
             * This test is only valid for interfaces bridged to WANs.
             * Non-WANs don't have a gateway so there is no "backwards"
             */
            if ( ! master.getIsWan() )
                return;

            InetAddress gateway   = UvmContextFactory.context().networkManager().getInterfaceStatus( master.getInterfaceId() ).getV4Gateway();
            if (gateway == null) {
                logger.warn("Missing gateway on bridge master: " + master.getInterfaceId());
                return;
            }

            /**
             * Lookup gateway MAC using arp -a
             */
            String gatewayMac = this.execManager.execOutput( "arp -a " + gateway.getHostAddress() + " | awk '{print $4}' ");
            if ( gatewayMac == null ) {
                logger.warn("Unable to determine MAC for " + gateway.getHostAddress());
                return;
            }
            gatewayMac = gatewayMac.replaceAll("\\s+","");
            if ( "".equals(gatewayMac) || "entries".equals(gatewayMac)) {
                logger.warn("Unable to determine MAC for " + gateway.getHostAddress());
                return;
            }

            /**
             * Lookup gateway bridge port # using brctl showmacs
             */
            String portNo = this.execManager.execOutput( "brctl showmacs " + bridgeName + " | awk '/" + gatewayMac + "/ {print $1}'");
            if ( portNo == null) {
                logger.warn("Unable to find port number for MAC: " + gatewayMac);
                return;
            }
            portNo = portNo.replaceAll("\\s+","");
            if ( "".equals(portNo) ) {
                logger.warn("Unable to find port number for MAC: " + gatewayMac);
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
            InterfaceSettings gatewayIntf = UvmContextFactory.context().networkManager().findInterfaceSystemDev(gatewayInterfaceSystemName);
            if (gatewayIntf == null) {
                logger.warn("Unable to find gatewayIntf " + gatewayInterfaceSystemName);
                return;
            }
            logger.debug("testBridgeBackwards: Final Gateway Inteface: " + gatewayIntf.getName() + " is WAN: " + gatewayIntf.getIsWan());

            /**
             * Ideally, this is the WAN, however if its actually an interface bridged to a WAN, then the interfaces are probably backwards
             */
            if (!gatewayIntf.getIsWan()) {
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
                alertText += gateway.getHostAddress();
                alertText += ") ";
                alertText += i18nUtil.tr("is on") + " ";
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
        for ( InterfaceSettings intf : UvmContextFactory.context().networkManager().getEnabledInterfaces() ) {
            if ( intf.getSystemDev() == null )
                continue;

            String lines = this.execManager.execOutput( "ifconfig " + intf.getPhysicalDev() + " | awk '/errors/ {print $3}'");
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
        List<Node> spamBlockerLiteList = UvmContextFactory.context().nodeManager().nodeInstances("untangle-node-spam-blocker-lite");
        List<Node> spamblockerList = UvmContextFactory.context().nodeManager().nodeInstances("untangle-node-spamblocker");
        String nodeName = "Spam Blocker";

        if (spamBlockerLiteList.size() == 0 && spamblockerList.size() == 0)
            return;
        if (spamBlockerLiteList.size() > 0)
            nodeName = "Spam Blocker Lite";
        if (spamblockerList.size() > 0)
            nodeName = "Spam Blocker";

        for ( InterfaceSettings intf : UvmContextFactory.context().networkManager().getEnabledInterfaces() ) {
            if (!intf.getIsWan())
                continue;

            InetAddress dnsPrimary   = UvmContextFactory.context().networkManager().getInterfaceStatus( intf.getInterfaceId() ).getV4Dns1();
            InetAddress dnsSecondary = UvmContextFactory.context().networkManager().getInterfaceStatus( intf.getInterfaceId() ).getV4Dns2();

            List<String> dnsServers = new LinkedList<String>();
            if ( dnsPrimary != null ) dnsServers.add(dnsPrimary.getHostAddress());
            if ( dnsSecondary != null ) dnsServers.add(dnsSecondary.getHostAddress());

            for (String dnsServer : dnsServers) {
                /* hardcode common known bad DNS */
                if ( "8.8.8.8".equals( dnsServer ) || /* google */
                     "8.8.4.4".equals( dnsServer ) || /* google */
                     "4.2.2.1".equals( dnsServer ) || /* level3 */
                     "4.2.2.2".equals( dnsServer ) || /* level3 */
                     "208.67.222.222".equals( dnsServer ) || /* openDNS */
                     "208.67.222.220".equals( dnsServer ) /* openDNS */ ) {
                    String alertText = "";
                    alertText += nodeName + " " + i18nUtil.tr("is installed but an unsupported DNS server is used");
                    alertText += " (";
                    alertText += intf.getName();
                    alertText += ", ";
                    alertText += dnsServer;
                    alertText += ").";

                    alertList.add(alertText);
                }
                /* otherwise check each DNS against spamhaus */
                else {
                    Lookup lookup;
                    Record[] records = null;
                    InetAddress expectedResult;

                    try {
                        lookup = new Lookup("2.0.0.127.zen.spamhaus.org");
                        expectedResult = InetAddress.getByName("127.0.0.4");
                    } catch ( Exception e ) {
                        logger.warn( "Invalid Lookup", e );
                        continue;
                    }
                    try {
                        lookup.setResolver( new SimpleResolver( dnsServer ) );
                        records = lookup.run();
                    } catch (Exception e) {
                        logger.warn("Invalid Resolver: " + dnsServer );
                    }

                    if ( records == null ) {
                        records = new Record[0];
                    }

                    boolean found = false;

                    found = false;
                    for (Record r : records) {
                        if (r instanceof ARecord) {
                            InetAddress addr = ((ARecord)r).getAddress();
                            if ( addr != null && addr.equals( expectedResult ) )
                                found = true;
                        }
                    }

                    if ( !found ) {
                        String alertText = "";
                        alertText += nodeName + " " + i18nUtil.tr("is installed but a DNS server");
                        alertText += " (";
                        alertText += intf.getName();
                        alertText += ", ";
                        alertText += dnsServer + ") ";
                        alertText += i18nUtil.tr("fails to resolve DNSBL queries.");

                        alertList.add(alertText);
                    }
                }
            }
        }
    }

    /**
     * Tests that zvelo queries can be resolved correctly
     */
    @SuppressWarnings("rawtypes")
    private void testZveloDNSServers(List<String> alertList)
    {
        List<Node> webFilterList = UvmContextFactory.context().nodeManager().nodeInstances("untangle-node-web-filter");

        if ( webFilterList.size() == 0 )
            return;

        String query = null;
        try {
            Method method;
            Node webFilter = webFilterList.get(0);

            Class[] args = { String.class, String.class };
            method = webFilter.getClass().getMethod( "encodeDnsQuery", args );
            query = (String) method.invoke( webFilter, "cnn.com", "/" );
        } catch (Exception e) {
            logger.warn("Exception generating Web Filter DNS query: ",e);
            return;
        }

        if ( query == null ) {
            logger.warn("Invalid zvelo query: " + query);
            return;
        }

        for ( InterfaceSettings intf : UvmContextFactory.context().networkManager().getEnabledInterfaces() ) {
            if (!intf.getIsWan())
                continue;

            InetAddress dnsPrimary   = UvmContextFactory.context().networkManager().getInterfaceStatus( intf.getInterfaceId() ).getV4Dns1();
            InetAddress dnsSecondary = UvmContextFactory.context().networkManager().getInterfaceStatus( intf.getInterfaceId() ).getV4Dns2();

            List<String> dnsServers = new LinkedList<String>();
            if ( dnsPrimary != null ) dnsServers.add(dnsPrimary.getHostAddress());
            if ( dnsSecondary != null ) dnsServers.add(dnsSecondary.getHostAddress());

            for (String dnsServer : dnsServers) {

                Lookup lookup;
                Record[] records = null;

                try {
                    lookup = new Lookup( query, Type.TXT );
                } catch ( Exception e ) {
                    logger.warn( "Invalid Lookup", e );
                    continue;
                }
                long t0 = System.currentTimeMillis();
                try {
                    lookup.setResolver( new SimpleResolver( dnsServer ) );
                    records = lookup.run();
                } catch (Exception e) {
                    logger.warn("Invalid Resolver: " + dnsServer );
                }
                long t1 = System.currentTimeMillis();

                if ( records == null ) {
                    records = new Record[0];
                }

                boolean found = false;
                for (Record r : records) {
                    if (r instanceof TXTRecord) {
                        for (Object o : ((TXTRecord)r).getStringsAsByteArrays()) {
                            String resultStr = new String((byte[])o);
                            //if there is a TXT response that includes cnn.com its probably correct
                            if (resultStr.contains("cnn.com"))
                                found = true;
                        }
                    }
                }

                if (!found) {
                    String alertText = "";
                    alertText += "Web Filter " + i18nUtil.tr("is installed but a DNS server");
                    alertText += " (";
                    alertText += intf.getName();
                    alertText += ", ";
                    alertText += dnsServer + ")";
                    alertText += " " + i18nUtil.tr("fails to resolve categorization queries.");

                    logger.warn("DNS Lookup failed [" + dnsServer + ",TXT]: " + query);
                    alertList.add(alertText);
                } else if ( t1-t0 > 500 ) {
                    String alertText = "";
                    alertText += i18nUtil.tr("A DNS server responds slowly.");
                    alertText += " (";
                    alertText += intf.getName();
                    alertText += ", ";
                    alertText += dnsServer;
                    alertText += ", ";
                    alertText += (t1-t0) + " ms";
                    alertText += ") ";
                    alertText += i18nUtil.tr("This may negatively effect Web Filter performance.");

                    alertList.add(alertText);
                }
            }
        }
    }

    /**
     * This test that the event writing time on average is not "too" slow.
     */
    private void testEventWriteTime(List<String> alertList)
    {
        final double MAX_AVG_TIME_WARN = 50.0;

        Reporting reports = (Reporting) UvmContextFactory.context().nodeManager().node("untangle-node-reports");
        /* if reports not installed - no events - just return */
        if (reports == null)
            return;

        double avgTime = reports.getAvgWriteTimePerEvent();
        if (avgTime > MAX_AVG_TIME_WARN) {
            String alertText = "";
            alertText += i18nUtil.tr("Event processing is slow");
            alertText += " (";
            alertText += String.format("%.1f",avgTime) + " ms";
            alertText += "). ";

            alertList.add(alertText);
        }
    }

    /**
     * This test that the event writing delay is not too long
     */
    private void testEventWriteDelay(List<String> alertList)
    {
        final long MAX_TIME_DELAY_SEC = 600; /* 10 minutes */

        Reporting reports = (Reporting) UvmContextFactory.context().nodeManager().node("untangle-node-reports");
        /* if reports not installed - no events - just return */
        if (reports == null)
            return;

        long delay = reports.getWriteDelaySec();
        if (delay > MAX_TIME_DELAY_SEC) {
            String alertText = "";
            alertText += i18nUtil.tr("Event processing is behind");
            alertText += " (";
            alertText += String.format("%.1f",(((float)delay)/60.0)) + " minute delay";
            alertText += "). ";

            alertList.add(alertText);
        }
    }

    /**
     * This test tests for "nf_queue full" messages in kern.log
     */
    private void testQueueFullMessages(List<String> alertList)
    {
        int result = this.execManager.execResult("tail -n 20 /var/log/kern.log | grep -q 'nf_queue:.*dropping packets'");
        if ( result == 0 ) {
            String alertText = "";
            alertText += i18nUtil.tr("Packet processing recently overloaded.");

            alertList.add(alertText);
        }
    }

    /**
     * This test that the shield is enabled
     */
    private void testShieldEnabled( List<String> alertList )
    {
        Node shield = UvmContextFactory.context().nodeManager().node("untangle-node-shield");
        String alertText = "";
        alertText += i18nUtil.tr("The shield is disabled. This can cause performance and stability problems.");

        if ( shield.getRunState() != NodeSettings.NodeState.RUNNING ) {
            alertList.add(alertText);
            return;
        }

        try {
            java.lang.reflect.Method method;
            method = shield.getClass().getMethod( "getSettings" );
            Object settings = method.invoke( shield );
            method = settings.getClass().getMethod( "isShieldEnabled" );
            Boolean result = (Boolean) method.invoke( settings );
            if (! result ) {
                alertList.add(alertText);
                return;
            }
        } catch (Exception e) {
            logger.warn("Exception reading shield settings: ",e);
        }
    }

    private void testRoutesToReachableAddresses( List<String> alertList )
    {
        int result;
        List<StaticRoute> routes = UvmContextFactory.context().networkManager().getNetworkSettings().getStaticRoutes();

        for ( StaticRoute route : routes ) {
            if ( ! route.getToAddr() )
                continue;

            /**
             * If already in the ARP table, continue
             */
            result = this.execManager.execResult("arp -n " + route.getNextHop() + " | grep -q HWaddress");
            if ( result == 0 )
                continue;

            /**
             * If not, force arp resolution with ping
             * Then recheck ARP table
             */
            result = this.execManager.execResult("ping -c1 -W1 " + route.getNextHop());
            result = this.execManager.execResult("arp -n " + route.getNextHop() + " | grep -q HWaddress");
            if ( result == 0 )
                continue;

            String alertText = "";
            alertText += i18nUtil.tr("Route to unreachable address:");
            alertText += " ";
            alertText += route.getNextHop();

            alertList.add(alertText);
        }

    }

    private void testServerConf( List<String> alertList )
    {
        try {
            String arch = System.getProperty("sun.arch.data.model") ;

            // only check 64-bit machines
            if ( arch == null || ! "64".equals( arch ) )
                return;

            // check total memory, return if unable to check total memory
            String result = this.execManager.execOutput( "awk '/MemTotal:/ {print $2}' /proc/meminfo" );
            if ( result == null )
                return;
            result = result.trim();
            if ( "".equals(result) )
                return;

            int memTotal = Integer.parseInt( result );
            if ( memTotal < 1900000 ) {
                String alertText = i18nUtil.tr("Running 64-bit with less than 2 gigabytes RAM is not suggested.");
                alertList.add(alertText);
            }
        } catch (Exception e) {
            logger.warn("Exception testing system: ",e);
        }
    }

    private void testLicenseCompliance( List<String> alertList )
    {
        int currentSize = UvmContextFactory.context().hostTable().getCurrentActiveSize();
        int seatLimit = UvmContextFactory.context().licenseManager().getSeatLimit( true );
        int actualSeatLimit = UvmContextFactory.context().licenseManager().getSeatLimit( false );

        if ( seatLimit > 0 && currentSize > seatLimit ) {
            String alertText = i18nUtil.tr("Currently the number of devices significantly exceeds the number of licensed devices.") + " (" + currentSize + " > " + actualSeatLimit + ")";
            alertList.add(alertText);
        }
     }
        

}
