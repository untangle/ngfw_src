/**
 * $Id: IpsecVpnApp.java 41228 2015-09-11 22:45:38Z dmorris $
 */

package com.untangle.app.ipsec_vpn;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Timer;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jabsorb.JSONSerializer;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.ExecManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.app.License;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.util.I18nUtil;

public class IpsecVpnApp extends AppBase
{
    private final static String GRAB_LOGFILE_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-logfile";
    private final static String GRAB_VIRTUALLOGFILE_SCRIPT = System.getProperty("uvm.home") + "/bin/l2tpd-logfile";
    private final static String GRAB_POLICY_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-policy";
    private final static String GRAB_STATE_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-state";
    private final static String GRAB_STATUS_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-status";
    private final static String GRAB_TRAFFIC_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-tunnel-stats";
    private final static String APP_STARTUP_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-app-startup";

    private final static String STRONGSWAN_STROKE_CONFIG = "/etc/strongswan.d/charon/stroke.conf";
    private final static String STRONGSWAN_STROKE_TIMEOUT = "15000";

    private static final String STAT_CONFIGURED = "configured";
    private static final String STAT_ENABLED = "enabled";
    private static final String STAT_DISABLED = "disabled";
    private static final String STAT_VIRTUAL = "virtual";

    private static final Logger logger = Logger.getLogger(IpsecVpnApp.class);
    private final VirtualUserTable virtualUserTable = new VirtualUserTable();
    private final Integer policyId = getAppSettings().getPolicyId();
    private final PipelineConnector[] connectors = new PipelineConnector[0];
    private final IpsecVpnManager manager = new IpsecVpnManager();

    protected static ExecManager execManager = null;

    private enum MatchMode
    {
        STATE, IN, OUT, FWD
    }

    protected IpsecVpnSettings settings;
    protected Timer timer;

    public IpsecVpnApp(com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties)
    {
        super(appSettings, appProperties);

        logger.debug("IpsecVpnApp()");

        this.addMetric(new AppMetric(STAT_CONFIGURED, I18nUtil.marktr("Configured Tunnels")));
        this.addMetric(new AppMetric(STAT_DISABLED, I18nUtil.marktr("Disabled Tunnels")));
        this.addMetric(new AppMetric(STAT_ENABLED, I18nUtil.marktr("Enabled Tunnels")));
        this.addMetric(new AppMetric(STAT_VIRTUAL, I18nUtil.marktr("VPN Clients")));

        try {
            fixStrongswanConfig();
        }

        catch (Exception exn) {
            logger.warn("Unable to update strongswan config files");
        }
    }

    @Override
    public void initializeSettings()
    {
        logger.debug("initializeSettings()");

        settings = new IpsecVpnSettings();
        LinkedList<VirtualListen> listenList = new LinkedList<VirtualListen>();

        // listen on the first WAN interface for the initial L2TP config
        InetAddress firstWan = UvmContextFactory.context().networkManager().getFirstWanAddress();
        if (firstWan != null) {
            VirtualListen item = new VirtualListen();
            item.setAddress(firstWan.getHostAddress().toString());
            listenList.add(item);
        }

        settings.setVirtualListenList(listenList);

        LinkedList<IpsecVpnTunnel> tunnelList = new LinkedList<IpsecVpnTunnel>();
        IpsecVpnTunnel tmp;

        tmp = new IpsecVpnTunnel();
        tmp.setId(1);
        tmp.setActive(false);
        tmp.setConntype("tunnel");
        tmp.setDescription("Example 1");
        tmp.setSecret("NOTICEhowWEuseAniceLONGstringINthisEXAMPLEwhichWILLbeMUCHmoreSECUREthanAsingleWORD");
        tmp.setRunmode("start");
        tmp.setLeft("198.51.100.1");
        tmp.setLeftSubnet("192.168.101.0/24");
        tmp.setRight("203.0.113.1");
        tmp.setRightSubnet("192.168.102.0/24");
        tunnelList.add(tmp);

        tmp = new IpsecVpnTunnel();
        tmp.setId(2);
        tmp.setActive(false);
        tmp.setConntype("tunnel");
        tmp.setDescription("Example 2");
        tmp.setSecret("thisISanotherGREATexampleOFaPREsharedSECRETthatISveryLONGandTHUSreasonablySECURE");
        tmp.setRunmode("start");
        tmp.setLeft("198.51.100.1");
        tmp.setLeftSubnet("10.10.0.0/16");
        tmp.setRight("203.0.113.1");
        tmp.setRightSubnet("10.20.0.0/16");
        tunnelList.add(tmp);

        settings.setTunnels(tunnelList);
        setSettings(settings);
    }

    public IpsecVpnSettings getSettings()
    {
        logger.debug("getSettings()");
        return (settings);
    }

    public void setSettings(IpsecVpnSettings newSettings)
    {
        int idx;

        logger.debug("setSettings()");

        IPMaskedAddress lNet = new IPMaskedAddress(newSettings.getVirtualAddressPool());
        IPMaskedAddress xNet = new IPMaskedAddress(newSettings.getVirtualXauthPool());
        IPMaskedAddress gNet = new IPMaskedAddress(newSettings.getVirtualNetworkPool());

        if (lNet.isIntersecting(xNet)) {
            throw new RuntimeException("Detected conflict between L2TP (" + lNet + ") and Xauth (" + xNet + ") address pools.");
        }

        if (lNet.isIntersecting(gNet)) {
            throw new RuntimeException("Detected conflict between L2TP (" + lNet + ") and GRE (" + gNet + ") address pools.");
        }

        if (xNet.isIntersecting(gNet)) {
            throw new RuntimeException("Detected conflict between Xauth (" + xNet + ") and GRE (" + gNet + ") address pools.");
        }

        idx = 0;

        for (IpsecVpnTunnel tunnel : newSettings.getTunnels()) {
            tunnel.setId(++idx);
        }

        idx = 0;

        for (IpsecVpnNetwork network : newSettings.getNetworks()) {
            network.setId(++idx);
        }

        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = getAppSettings().getId().toString();

        try {
            settingsManager.save(System.getProperty("uvm.settings.dir") + "/ipsec-vpn/settings_" + appID + ".js", newSettings);
        } catch (Exception exn) {
            logger.error("Failed to save settings: ", exn);
            return;
        }

        this.settings = newSettings;
        reconfigure();
    }

    public String getLogFile()
    {
        logger.debug("getLogFile()");
        return IpsecVpnApp.execManager().execOutput(GRAB_LOGFILE_SCRIPT);
    }

    public String getVirtualLogFile()
    {
        logger.debug("getVirtualLogFile()");
        return IpsecVpnApp.execManager().execOutput(GRAB_VIRTUALLOGFILE_SCRIPT);
    }

    public String getPolicyInfo()
    {
        logger.debug("getPolicyInfo()");
        return IpsecVpnApp.execManager().execOutput(GRAB_POLICY_SCRIPT);
    }

    public String getStateInfo()
    {
        logger.debug("getStateInfo()");
        return IpsecVpnApp.execManager().execOutput(GRAB_STATE_SCRIPT);
    }

    protected static ExecManager execManager()
    {
        if (IpsecVpnApp.execManager != null) return IpsecVpnApp.execManager;

        logger.warn("IpsecVpn execManager not initialized, using global execManager.");
        return UvmContextFactory.context().execManager();
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        logger.debug("getConnectors()");
        return this.connectors;
    }

    @Override
    protected void postInit()
    {
        logger.debug("postInit()");
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = getAppSettings().getId().toString();
        IpsecVpnSettings readSettings = null;
        String settingsFilename = System.getProperty("uvm.settings.dir") + "/ipsec-vpn/settings_" + appID + ".js";

        try {
            readSettings = settingsManager.load(IpsecVpnSettings.class, settingsFilename);
        } catch (Exception exn) {
            logger.error("Failed to load settings: ", exn);
        }

        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings");
            this.initializeSettings();
        } else {
            logger.info("Loaded settings from: " + settingsFilename);
            this.settings = readSettings;
            reconfigure();
        }
    }

    @Override
    protected void preStart( boolean isPermanentTransition )
    {
        logger.debug("preStart()");

        if (IpsecVpnApp.execManager == null) {
            IpsecVpnApp.execManager = UvmContextFactory.context().createExecManager();
            IpsecVpnApp.execManager.setLevel(org.apache.log4j.Level.DEBUG);
            IpsecVpnApp.execManager.exec(APP_STARTUP_SCRIPT);
        }

        if (isLicenseValid() != true) throw (new RuntimeException("Unable to start ipsec-vpn service: invalid license"));

        UvmContextFactory.context().daemonManager().incrementUsageCount("xl2tpd");
        UvmContextFactory.context().daemonManager().incrementUsageCount("ipsec");

        reconfigure();
    }

    @Override
    protected void postStart( boolean isPermanentTransition )
    {
        logger.debug("postStart()");

        // our timer class expects to be called once every minute
        timer = new Timer();
        timer.schedule(new IpsecVpnTimer(this), 60000, 60000);
    }

    @Override
    protected void preStop( boolean isPermanentTransition )
    {
        logger.debug("preStop()");

        super.preStop( isPermanentTransition );

        timer.cancel();

        int counter = 0;

        for (VirtualUserEntry entry : virtualUserTable.buildUserList()) {
            logger.info("Disconnecting L2TP client " + entry.getClientUsername() + " address " + entry.getClientAddress().getHostAddress());
            IpsecVpnApp.execManager().exec("kill -HUP " + entry.getNetProcess());
            counter++;
        }

        // if there were any disconnects wait a couple seconds for the
        // ip-down script to update the app with the session statistics
        if (counter > 0) {
            try {
                Thread.sleep(2000);
            } catch (Exception exn) {
            }
        }
    }

    @Override
    protected void postStop( boolean isPermanentTransition )
    {
        logger.debug("postStop()");

        if (IpsecVpnApp.execManager != null) {
            IpsecVpnApp.execManager.close();
            IpsecVpnApp.execManager = null;
        }

        UvmContextFactory.context().daemonManager().decrementUsageCount("xl2tpd");
        UvmContextFactory.context().daemonManager().decrementUsageCount("ipsec");
    }

    private synchronized void reconfigure()
    {
        logger.debug("reconfigure()");
        manager.generateConfig(this.settings);
        updateBlingers();

        ExecManagerResult result;

        /**
         * Need to run iptables rules, they may already be there, but they might
         * not be so this is safe to run anytime and it will insert the rules if
         * not present
         */
        result = UvmContextFactory.context().execManager().exec("/etc/untangle-netd/iptables-rules.d/710-ipsec");
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("/etc/untangle-netd/iptables-rules.d/710-ipsec" + ": " + result.getResult());
            for (String line : lines)
                logger.info("/etc/untangle-netd/iptables-rules.d/710-ipsec" + ": " + line);
        } catch (Exception e) {
        }

        result = UvmContextFactory.context().execManager().exec("/etc/untangle-netd/iptables-rules.d/711-xauth");
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("/etc/untangle-netd/iptables-rules.d/711-xauth" + ": " + result.getResult());
            for (String line : lines)
                logger.info("/etc/untangle-netd/iptables-rules.d/711-xauth" + ": " + line);
        } catch (Exception e) {
        }

        result = UvmContextFactory.context().execManager().exec("/etc/untangle-netd/iptables-rules.d/712-gre");
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("/etc/untangle-netd/iptables-rules.d/712-gre" + ": " + result.getResult());
            for (String line : lines)
                logger.info("/etc/untangle-netd/iptables-rules.d/712-gre" + ": " + line);
        } catch (Exception e) {
        }
    }

    public void updateBlingers()
    {
        logger.debug("updateBlingers()");
        LinkedList<IpsecVpnTunnel> list = settings.getTunnels();
        int dtot = 0;
        int etot = 0;
        int ttot = 0;

        if (list == null) return;
        ttot = list.size();

        for (int x = 0; x < ttot; x++) {
            if (list.get(x).getActive() == true) etot++;
            else
                dtot++;
        }

        this.setMetric(IpsecVpnApp.STAT_CONFIGURED, (long) ttot);
        this.setMetric(IpsecVpnApp.STAT_DISABLED, (long) dtot);
        this.setMetric(IpsecVpnApp.STAT_ENABLED, (long) etot);
        this.setMetric(IpsecVpnApp.STAT_VIRTUAL, virtualUserTable.countVirtualUsers());
    }

    public boolean isLicenseValid()
    {
        logger.debug("isLicenseValid()");
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.IPSEC_VPN)) return true;
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.IPSEC_VPN_OLDNAME)) return true;
        return false;
    }

    public int virtualUserConnect(String clientProtocol, InetAddress clientAddress, String clientUsername, String netInterface, String netProcess)
    {
        logger.debug("virtualUserConnect PROTO:" + clientProtocol + " ADDR:" + clientAddress.getHostAddress() + " USER:" + clientUsername + " IF:" + netInterface + " PROC:" + netProcess);

        // put the client in the virtual user table
        VirtualUserEntry entry = virtualUserTable.insertVirtualUser(clientProtocol, clientAddress, clientUsername, netInterface, netProcess);

        // log the event in the database and save the event object for later
        VirtualUserEvent event = new VirtualUserEvent(clientAddress, clientProtocol, clientUsername, netInterface, netProcess);
        logEvent(event);
        logger.debug("virtualUserConnect(logEvent) " + event.toString());

        entry.pushEventHolder(event);

        updateBlingers();
        return (0);
    }

    public int virtualUserGoodbye(String clientProtocol, InetAddress clientAddress, String clientUsername, String netRXcount, String netTXcount)
    {
        logger.debug("virtualUserGoodbye PROTO:" + clientProtocol + " ADDR:" + clientAddress.getHostAddress() + " USER:" + clientUsername + " RX:" + netRXcount + " TX:" + netTXcount);

        // make sure the client exists in the user table
        VirtualUserEntry entry = virtualUserTable.searchVirtualUser(clientAddress);
        if (entry == null) return (1);

        // update the user event in the database
        VirtualUserEvent event = entry.grabEventHolder();

        String elapsed = new String();
        long total = (entry.getSessionElapsed() / 1000);
        long hh = ((total / 3600) % 24);
        long mm = ((total / 60) % 60);
        long ss = (total % 60);

        if (hh < 10) elapsed += "0";
        elapsed += String.valueOf(hh);
        elapsed += ":";
        if (mm < 10) elapsed += "0";
        elapsed += String.valueOf(mm);
        elapsed += ":";
        if (ss < 10) elapsed += "0";
        elapsed += String.valueOf(ss);

        event.updateEvent(elapsed, new Long(netRXcount), new Long(netTXcount));
        logEvent(event);
        logger.debug("virtualUserGoodbye(logEvent) " + event.toString());

        virtualUserTable.removeVirtualUser(clientAddress);

        updateBlingers();
        return (0);
    }

    public int virtualUserDisconnect(InetAddress clientAddress, String clientUsername)
    {
        logger.debug("virtualUserDisconnect ADDR:" + clientAddress.getHostAddress() + " USER:" + clientUsername);

        // make sure the client exists in the user table
        VirtualUserEntry entry = virtualUserTable.searchVirtualUser(clientAddress);
        if (entry == null) return (1);

        // for L2TP clients we send a HUP signal to the pppd process
        if (entry.getClientProtocol().equals("L2TP")) {
            IpsecVpnApp.execManager().exec("kill -HUP " + entry.getNetProcess());
        }

        // for Xauth clients we call ipsec down using the connection and unique id
        if (entry.getClientProtocol().equals("XAUTH")) {
            IpsecVpnApp.execManager().exec("ipsec down " + entry.getNetInterface() + "[" + entry.getNetProcess() + "]");
        }

        // for IKEv2 clients we call ipsec down using the connection and unique id
        if (entry.getClientProtocol().equals("IKEV2")) {
            IpsecVpnApp.execManager().exec("ipsec down " + entry.getNetInterface() + "[" + entry.getNetProcess() + "]");
        }

        return (0);
    }

    public LinkedList<VirtualUserEntry> getVirtualUsers()
    {
        logger.debug("getVirtualUsers()");
        return (virtualUserTable.buildUserList());
    }

    public LinkedList<ConnectionStatusRecord> getTunnelStatus()
    {
        LinkedList<ConnectionStatusRecord> displayList = new LinkedList<ConnectionStatusRecord>();
        LinkedList<ConnectionStatusRecord> statusList;

        String output = IpsecVpnApp.execManager().execOutput(GRAB_STATUS_SCRIPT);

        // call the ipsec-status script to get the state and policy info
        try {
            JSONSerializer serializer = new JSONSerializer();

            serializer.setFixupDuplicates(false);
            serializer.setMarshallNullAttributes(false);
            serializer.registerDefaultSerializers();

            @SuppressWarnings("unchecked")
            LinkedList<ConnectionStatusRecord> fetchList = (LinkedList<ConnectionStatusRecord>) serializer.fromJSON(output);
            statusList = fetchList;
        } catch (Exception exn) {
            logger.error("Invalid JSON returned from ipsec-status: " + output, exn);
            return (displayList);
        }

        // get the list of configured tunnels from the settings
        LinkedList<IpsecVpnTunnel> configList = settings.getTunnels();
        if (configList == null) return (displayList);

        // create a status display record for all enabled tunnels
        for (int x = 0; x < configList.size(); x++) {
            IpsecVpnTunnel tunnel = configList.get(x);
            if (tunnel.getActive() == false) continue;
            ConnectionStatusRecord record = createDisplayRecord(tunnel, statusList);
            displayList.add(record);
        }

        return (displayList);
    }

    private ConnectionStatusRecord createDisplayRecord(IpsecVpnTunnel tunnel, LinkedList<ConnectionStatusRecord> statusList)
    {
        ConnectionStatusRecord record = new ConnectionStatusRecord();
        ConnectionStatusRecord finder;
        AddressCalculator srcCalc;
        AddressCalculator dstCalc;
        String remoteAddress;

        // start by creating an inactive record using the configured values
        record.setType("DISPLAY");
        record.setId(Integer.toString(tunnel.getId()));
        record.setDescription(tunnel.getDescription());
        record.setProto(tunnel.getDescription());
        record.setSrc(tunnel.getLeft());
        record.setDst(tunnel.getRight());
        record.setTmplSrc(tunnel.getLeftSubnet());
        record.setTmplDst(tunnel.getRightSubnet());
        record.setMode("inactive");
        record.setInBytes("0");
        record.setOutBytes("0");

        // make sure we have the remote IP address in case they used the hostname
        try {
            InetAddress remote = InetAddress.getByName(tunnel.getRight());
            remoteAddress = remote.getHostAddress().toString();
        } catch (Exception exn) {
            record.setMode("unknown");
            return (record);
        }

        for (int x = 0; x < statusList.size(); x++) {
            // walk through every status record until we find the STATE
            // record that matches the argumented tunnel entry.
            ConnectionStatusRecord status = statusList.get(x);

            // start with the STATE record where src matches the local IP
            // and dst matches the remote IP of the configured tunnel
            if (!status.getType().equals("STATE")) continue;
            if (!status.getMode().equals(tunnel.getConntype())) continue;
            if (!status.getSrc().equals(tunnel.getLeft())) continue;
            if (!status.getDst().equals(remoteAddress)) continue;

            // we found the correct STATE record so now we have make sure there
            // is a matching STATE record for the reverse direction along with
            // matching IN, OUT, and FWD policy records.  We do these searches by
            // calling the findMatchingRecord function which searches through the
            // entire list of status records for a match based on the status
            // record we're currently evaluating.  Think double-nested for loop.

            // look for the inverse STATE record where the src matches the
            // remote IP and the dst matches the local IP and the requid
            // matches the reqid in the record we found above
            finder = findMatchingRecord(MatchMode.STATE, status.getReqid(), remoteAddress, tunnel.getLeft(), statusList);
            if (finder == null) continue;

            // because we allow, and ipsec seems to accept, subnets that are defined
            // using the host/bits format (192.168.2.1/24) in addition to the
            // network/bits format (192.168.2.0/24) we use the IpPalculator to
            // convert the configured subnet values to the actual network/bits
            // values needed to match the 'ip xfrm policy' output
            srcCalc = new AddressCalculator(tunnel.getLeftSubnet());
            dstCalc = new AddressCalculator(tunnel.getRightSubnet());

            // look for a POLICY out record with matching reqid that also matches our src and dst networks
            finder = findMatchingRecord(MatchMode.OUT, status.getReqid(), srcCalc.getBaseNetwork(), dstCalc.getBaseNetwork(), statusList);
            if (finder == null) continue;

            // look for a POLICY in record with matching reqid that also matches our dst and src networks
            finder = findMatchingRecord(MatchMode.IN, status.getReqid(), dstCalc.getBaseNetwork(), srcCalc.getBaseNetwork(), statusList);
            if (finder == null) continue;

            // for transport mode there won't be a POLICY fwd record so we're done
            // when we find the correct status record with matching in/out records
            if (tunnel.getConntype().equals("transport")) {
                record.setMode("active");
                getTrafficStatistics(record);
                break;
            }

            // look for a POLICY fwd record with matching reqid - we don't care about
            // src or dst here since if we make it this far we know the reqid of the
            // status record has matching in and out records
            finder = findMatchingRecord(MatchMode.FWD, status.getReqid(), null, null, statusList);
            if (finder == null) continue;

            // we found the correct status record with matching in/out/fwd records which means
            // the tunnel is active so we update the DISPLAY record with fields from the
            // POLICY fwd record inverting the src and dst to represent our perspective
            record.setSrc(finder.getTmplDst());
            record.setDst(finder.getTmplSrc());
            record.setTmplSrc(finder.getDst());
            record.setTmplDst(finder.getSrc());
            record.setMode("active");
            getTrafficStatistics(record);
            break;
        }

        return (record);
    }

    private ConnectionStatusRecord findMatchingRecord(MatchMode matchMode, String reqid, String src, String dst, LinkedList<ConnectionStatusRecord> statusList)
    {
        for (int x = 0; x < statusList.size(); x++) {
            ConnectionStatusRecord status = statusList.get(x);

            switch (matchMode)
            {
            case STATE:
                if (status.getType().equals("STATE") != true) break;
                if (status.getReqid().equals(reqid) != true) break;
                if (status.getSrc().equals(src) != true) break;
                if (status.getDst().equals(dst) != true) break;
                return (status);
            case IN:
                if (status.getType().equals("POLICY") != true) break;
                if (status.getDir().equals("in") != true) break;
                if (status.getReqid().equals(reqid) != true) break;
                if (status.getSrc().equals(src) != true) break;
                if (status.getDst().equals(dst) != true) break;
                return (status);
            case OUT:
                if (status.getType().equals("POLICY") != true) break;
                if (status.getDir().equals("out") != true) break;
                if (status.getReqid().equals(reqid) != true) break;
                if (status.getSrc().equals(src) != true) break;
                if (status.getDst().equals(dst) != true) break;
                return (status);
            case FWD:
                if (status.getType().equals("POLICY") != true) break;
                if (status.getDir().equals("fwd") != true) break;
                if (status.getReqid().equals(reqid) != true) break;
                return (status);
            }
        }
        return null;
    }

    private void getTrafficStatistics(ConnectionStatusRecord status)
    {
        long outValue = 0;
        long inValue = 0;
        int top, len;

        // Use the id and description to create a unique connection name that won't cause
        // problems in the ipsec.conf file by replacing non-word characters with a hyphen.
        // We also prefix this name with UT123_ to ensure no dupes in the config file.
        String workname = ("UT" + status.getId() + "_" + status.getDescription().replaceAll("\\W", "-"));

        /*
         * the script should return the tunnel stats in the following format |
         * TUNNNEL:tunnel_name IN:123 OUT:456 |
         */
        String result = IpsecVpnApp.execManager().execOutput(GRAB_TRAFFIC_SCRIPT + " " + workname);

        /*
         * We use the IN: and OUT: tags to find the beginning of each value and
         * the trailing space to isolate the numeric portions of the string to
         * keep Long.valueOf happy.
         */

        try {
            top = result.indexOf("IN:");
            if (top > 0) {
                len = result.substring(top + 3).indexOf(" ");
                if (len > 0) {
                    inValue = Long.valueOf(result.substring(top + 3, top + 3 + len));
                    status.setInBytes(Long.toString(inValue));
                }
            }

            top = result.indexOf("OUT:");
            if (top > 0) {
                len = result.substring(top + 4).indexOf(" ");
                if (len > 0) {
                    outValue = Long.valueOf(result.substring(top + 4, top + 4 + len));
                    status.setOutBytes(Long.toString(outValue));
                }
            }
        }

        /*
         * If we can't parse the tunnel stats just return
         */
        catch (Exception exn) {
            return;
        }
    }

    /*
     * https://lists.strongswan.org/pipermail/users/2013-June/004802.html
     */

    private void fixStrongswanConfig() throws Exception
    {
        File cfgfile = new File(STRONGSWAN_STROKE_CONFIG);
        StringBuffer buffer = new StringBuffer(1024);
        String line;

        FileReader fileReader = new FileReader(cfgfile);
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        while ((line = bufferedReader.readLine()) != null) {
            if (line.contains("timeout =") == true) {
                line = ("    timeout = " + STRONGSWAN_STROKE_TIMEOUT);
            }
            buffer.append(line);
            buffer.append("\n");
        }

        fileReader.close();

        FileWriter fileWriter = new FileWriter(STRONGSWAN_STROKE_CONFIG);
        fileWriter.write(buffer.toString());
        fileWriter.close();
    }
}
