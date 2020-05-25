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

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.NetspaceManager;
import com.untangle.uvm.NetspaceManager.IPVersion;
import com.untangle.uvm.NetspaceManager.NetworkSpace;
import com.untangle.uvm.SystemSettings;
import com.untangle.uvm.HookCallback;
import com.untangle.uvm.ExecManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.app.License;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.HostTableEntry;

/**
 * The IPsec application manages all IPsec tunnels and VPN configurations.
 * 
 * @author mahotz
 * 
 */

public class IpsecVpnApp extends AppBase
{
    private final static String GRAB_LOGFILE_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-logfile";
    private final static String GRAB_VIRTUAL_LOGFILE_SCRIPT = System.getProperty("uvm.home") + "/bin/l2tpd-logfile";
    private final static String GRAB_POLICY_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-policy";
    private final static String GRAB_STATE_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-state";
    private final static String GRAB_TUNNEL_STATUS_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-tunnel-status";
    private final static String APP_STARTUP_SCRIPT = System.getProperty("uvm.home") + "/bin/ipsec-app-startup";

    private final static String STRONGSWAN_STROKE_CONFIG = "/etc/strongswan.d/charon/stroke.conf";
    private final static String STRONGSWAN_STROKE_TIMEOUT = "15000";

    private static final String STAT_CONFIGURED = "configured";
    private static final String STAT_ENABLED = "enabled";
    private static final String STAT_DISABLED = "disabled";
    private static final String STAT_VIRTUAL = "virtual";

    private static final String NETSPACE_OWNER = "ipsec-vpn";
    private static final String NETSPACE_L2TP = "L2TP";
    private static final String NETSPACE_GRE = "GRE";
    private static final String NETSPACE_XAUTH = "Xauth";

    private static final Logger logger = Logger.getLogger(IpsecVpnApp.class);
    private final VirtualUserTable virtualUserTable = new VirtualUserTable();
    private final Integer policyId = getAppSettings().getPolicyId();
    private final PipelineConnector[] connectors = new PipelineConnector[0];
    private final IpsecVpnManager manager = new IpsecVpnManager();

    private final IpsecVpnHookCallback ipsecVpnHookCallback;
    protected static ExecManager execManager = null;

    private enum MatchMode
    {
        STATE, IN, OUT, FWD
    }

    protected IpsecVpnSettings settings;
    protected Timer dataTimer;
    protected Timer pingTimer;

    private String activeCertificate = UvmContextFactory.context().systemManager().getSettings().getIpsecCertificate();

    /**
     * Initializes the IPsec application by creating blingers and fixing low
     * level configuration options.
     * 
     * @param appSettings
     *        Application settings
     * @param appProperties
     *        Application properties
     */
    public IpsecVpnApp(com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties)
    {
        super(appSettings, appProperties);

        logger.debug("IpsecVpnApp()");

        this.ipsecVpnHookCallback = new IpsecVpnHookCallback();

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

    /**
     * Initialize application settings when no previous settings found.
     */
    @Override
    public void initializeSettings()
    {
        logger.debug("initializeSettings()");

        settings = new IpsecVpnSettings();
        LinkedList<VirtualListen> listenList = new LinkedList<>();

        // listen on the first WAN interface for the initial L2TP config
        InetAddress firstWan = UvmContextFactory.context().networkManager().getFirstWanAddress();
        if (firstWan != null) {
            VirtualListen item = new VirtualListen();
            item.setAddress(firstWan.getHostAddress().toString());
            listenList.add(item);
        }

        settings.setVirtualListenList(listenList);

        NetspaceManager nsmgr = UvmContextFactory.context().netspaceManager();
        LinkedList<IpsecVpnTunnel> tunnelList = new LinkedList<>();
        IpsecVpnTunnel tmp;

        tmp = new IpsecVpnTunnel();
        tmp.setId(1);
        tmp.setActive(false);
        tmp.setConntype("tunnel");
        tmp.setDescription("Example 1");
        tmp.setSecret("NOTICEhowWEuseAniceLONGstringINthisEXAMPLEwhichWILLbeMUCHmoreSECUREthanAsingleWORD");
        tmp.setRunmode("start");
        tmp.setLeft("198.51.100.1");
        tmp.setRight("203.0.113.1");

        tmp.setLeftSubnet(nsmgr.getAvailableAddressSpace(IPVersion.IPv4, 0, 24).toString());
        tmp.setRightSubnet(nsmgr.getAvailableAddressSpace(IPVersion.IPv4, 0, 24).toString());

        tunnelList.add(tmp);

        tmp = new IpsecVpnTunnel();
        tmp.setId(2);
        tmp.setActive(false);
        tmp.setConntype("tunnel");
        tmp.setDescription("Example 2");
        tmp.setSecret("thisISanotherGREATexampleOFaPREsharedSECRETthatISveryLONGandTHUSreasonablySECURE");
        tmp.setRunmode("start");
        tmp.setLeft("198.51.100.1");
        tmp.setRight("203.0.113.1");

        tmp.setLeftSubnet(nsmgr.getAvailableAddressSpace(IPVersion.IPv4, 0, 24).toString());
        tmp.setRightSubnet(nsmgr.getAvailableAddressSpace(IPVersion.IPv4, 0, 24).toString());

        tunnelList.add(tmp);

        //Setup default GRE/Xauth/L2TP addresses:
        settings.setVirtualNetworkPool(nsmgr.getAvailableAddressSpace(IPVersion.IPv4, 0, 24).toString());
        settings.setVirtualAddressPool(nsmgr.getAvailableAddressSpace(IPVersion.IPv4, 0, 24).toString());
        settings.setVirtualXauthPool(nsmgr.getAvailableAddressSpace(IPVersion.IPv4, 0, 24).toString());

        settings.setTunnels(tunnelList);
        setSettings(settings);
    }

    /**
     * Returns the application settings
     * 
     * @return The application settings
     */
    public IpsecVpnSettings getSettings()
    {
        logger.debug("getSettings()");
        return (settings);
    }

    /**
     * Set and apply new application settings.
     * 
     * @param newSettings
     *        The new settings
     */
    public void setSettings(IpsecVpnSettings newSettings)
    {
        int idx;

        logger.debug("setSettings()");

        /**
         * First we check for network address space conflicts
         */
        String conflict = checkNetworkReservations(newSettings);
        if (conflict != null) {
            throw new RuntimeException(conflict);
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

        /**
         * Change current settings and update network reservations any time
         * settings are saved.
         */
        this.settings = newSettings;
        updateNetworkReservations(newSettings);
        reconfigure();
    }

    /**
     * Gets the contents of the IPsec log file
     * 
     * @return The contents of the IPsec log file
     */
    public String getLogFile()
    {
        logger.debug("getLogFile()");
        return IpsecVpnApp.execManager().execOutput(GRAB_LOGFILE_SCRIPT);
    }

    /**
     * Gets the contents of the L2TP log file
     * 
     * @return The contents of the L2TP log file
     */
    public String getVirtualLogFile()
    {
        logger.debug("getVirtualLogFile()");
        return IpsecVpnApp.execManager().execOutput(GRAB_VIRTUAL_LOGFILE_SCRIPT);
    }

    /**
     * Gets the IPsec policy info returned by 'ip xfrm policy'
     * 
     * @return The IPsec policy info
     */
    public String getPolicyInfo()
    {
        logger.debug("getPolicyInfo()");
        return IpsecVpnApp.execManager().execOutput(GRAB_POLICY_SCRIPT);
    }

    /**
     * Gets the IPsec state info returned by 'ip xfrm state'
     * 
     * @return The IPsec state info
     */
    public String getStateInfo()
    {
        logger.debug("getStateInfo()");
        return IpsecVpnApp.execManager().execOutput(GRAB_STATE_SCRIPT);
    }

    /**
     * Returns our local execManager, or the global execManager if ours has not
     * yet been instantiated.
     * 
     * @return A valid execManager
     */
    protected static ExecManager execManager()
    {
        if (IpsecVpnApp.execManager != null) return IpsecVpnApp.execManager;

        logger.warn("IpsecVpn execManager not initialized, using global execManager.");
        return UvmContextFactory.context().execManager();
    }

    /**
     * Required by all UVM applications. We return an empty list of connectors
     * since this application doesn't do any traffic processing.
     * 
     * @return Our list of pipeline connectors.
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        logger.debug("getConnectors()");
        return this.connectors;
    }

    /**
     * After initialization we load and apply our settings, or create new
     * default settings if no saved settings are found.
     */
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

        /**
         * If we couldn't load settings, just initialize. We only need to
         * register network reservations on successful load since it will be
         * handled in saveSettings during initialize.
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings");
            this.initializeSettings();
        } else {
            logger.info("Loaded settings from: " + settingsFilename);
            this.settings = readSettings;
            updateNetworkReservations(readSettings);
            reconfigure();
        }
    }

    /**
     * Before the app is started we make sure our license is valid. If so, we
     * call our pre-start script and add xl2tpd and ipsec to the daemon manager.
     * 
     * @param isPermanentTransition
     */
    @Override
    protected void preStart(boolean isPermanentTransition)
    {
        logger.debug("preStart()");

        if (IpsecVpnApp.execManager == null) {
            IpsecVpnApp.execManager = UvmContextFactory.context().createExecManager();
            IpsecVpnApp.execManager.setLevel(org.apache.log4j.Level.DEBUG);
            IpsecVpnApp.execManager.exec(APP_STARTUP_SCRIPT);
        }

        if (isLicenseValid() != true) throw (new RuntimeException("Unable to start ipsec-vpn service: invalid license"));

        UvmContextFactory.context().hookManager().registerCallback(com.untangle.uvm.HookManager.UVM_SETTINGS_CHANGE, this.ipsecVpnHookCallback);

        UvmContextFactory.context().daemonManager().incrementUsageCount("xl2tpd");
        UvmContextFactory.context().daemonManager().incrementUsageCount("ipsec");

        reconfigure();
    }

    /**
     * After the app is started, we create our timer tasks
     * 
     * @param isPermanentTransition
     */
    @Override
    protected void postStart(boolean isPermanentTransition)
    {
        logger.debug("postStart()");

        // our DataTimer class expects to run every sixty (60) seconds
        dataTimer = new Timer();
        dataTimer.schedule(new IpsecVpnDataTimer(this), 60000, 60000);

        // our PingTimer class expects to run every twenty (20) seconds
        pingTimer = new Timer();
        pingTimer.schedule(new IpsecVpnPingTimer(this), 20000, 20000);
    }

    /**
     * Before the app is stopped we stop our monitoring timer task and
     * disconnect all active users.
     * 
     * @param isPermanentTransition
     */
    @Override
    protected void preStop(boolean isPermanentTransition)
    {
        logger.debug("preStop()");

        super.preStop(isPermanentTransition);

        UvmContextFactory.context().hookManager().unregisterCallback(com.untangle.uvm.HookManager.UVM_SETTINGS_CHANGE, this.ipsecVpnHookCallback);

        dataTimer.cancel();
        pingTimer.cancel();

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

    /**
     * After the app is stopped we remove xl2tpd and ipsec from the daemon
     * manager.
     * 
     * @param isPermanentTransition
     */
    @Override
    protected void postStop(boolean isPermanentTransition)
    {
        logger.debug("postStop()");

        if (IpsecVpnApp.execManager != null) {
            IpsecVpnApp.execManager.close();
            IpsecVpnApp.execManager = null;
        }

        UvmContextFactory.context().daemonManager().decrementUsageCount("xl2tpd");
        UvmContextFactory.context().daemonManager().decrementUsageCount("ipsec");
    }

    /**
     * Called to activate the node settings.
     */
    private synchronized void reconfigure()
    {
        logger.debug("reconfigure()");
        manager.generateConfig(this.settings, activeCertificate);
        updateBlingers();

        ExecManagerResult result;
        String script;

        /**
         * Need to run iptables rules, they may already be there, but they might
         * not be so this is safe to run anytime and it will insert the rules if
         * not present
         */
        script = (System.getProperty("prefix") + "/etc/untangle/iptables-rules.d/710-ipsec");
        result = UvmContextFactory.context().execManager().exec(script);
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info(script + ": " + result.getResult());
            for (String line : lines)
                logger.info(script + ": " + line);
        } catch (Exception e) {
        }

        script = (System.getProperty("prefix") + "/etc/untangle/iptables-rules.d/711-xauth");
        result = UvmContextFactory.context().execManager().exec(script);
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info(script + ": " + result.getResult());
            for (String line : lines)
                logger.info(script + ": " + line);
        } catch (Exception e) {
        }

        script = (System.getProperty("prefix") + "/etc/untangle/iptables-rules.d/712-gre");
        result = UvmContextFactory.context().execManager().exec(script);
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info(script + ": " + result.getResult());
            for (String line : lines)
                logger.info(script + ": " + line);
        } catch (Exception e) {
        }
    }

    /**
     * Updates the blinger display.
     */
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
            else dtot++;
        }

        this.setMetric(IpsecVpnApp.STAT_CONFIGURED, (long) ttot);
        this.setMetric(IpsecVpnApp.STAT_DISABLED, (long) dtot);
        this.setMetric(IpsecVpnApp.STAT_ENABLED, (long) etot);
        this.setMetric(IpsecVpnApp.STAT_VIRTUAL, virtualUserTable.countVirtualUsers());
    }

    /**
     * Checks to see if we have a valid license.
     * 
     * @return True if we have a valid license, otherwise false
     */
    public boolean isLicenseValid()
    {
        logger.debug("isLicenseValid()");
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.IPSEC_VPN)) return true;
        return false;
    }

    /**
     * Called externally by ipsec-virtual-user-event python script and possibly
     * others to register an active VPN user.
     * 
     * @param clientProtocol
     *        The protocol used to connect (L2TP | Xauth | IKEv2)
     * @param clientAddress
     *        The IP address assigned to the client
     * @param clientUsername
     *        The client username
     * @param netInterface
     *        The protocol interface assigned to the client
     * @param netProcess
     *        The process identifier assigned to the client
     * @return 0 for success
     */
    public int virtualUserConnect(String clientProtocol, InetAddress clientAddress, String clientUsername, String netInterface, String netProcess)
    {
        logger.debug("virtualUserConnect PROTO:" + clientProtocol + " ADDR:" + clientAddress.getHostAddress() + " USER:" + clientUsername + " IF:" + netInterface + " PROC:" + netProcess);

        /**
         * If concurrent logins are disabled we start by look for any existing
         * host table entry for the user.
         */
        if (getSettings().getAllowConcurrentLogins() == false) {
            HostTableEntry finder = UvmContextFactory.context().hostTable().findHostTableEntryByIpsecUsername(clientUsername);

            /**
             * If we found an entry and the IP address is different, we create a
             * new entry with the info from the old entry, remove the old entry,
             * and insert the new entry. It's the equivalent of updating the IP
             * address of the old entry, which we can't do directly.
             */
            if ((finder != null) && (finder.getAddress().equals(clientAddress) == false)) {
                logger.debug("Replacing host table entry for " + clientUsername + " OLD:" + finder.getAddress().getHostAddress().toString() + " NEW:" + clientAddress.getHostAddress().toString());
                HostTableEntry pusher = new HostTableEntry();
                pusher.copy(finder);
                pusher.setAddress(clientAddress);
                UvmContextFactory.context().hostTable().removeHostTableEntry(finder.getAddress());
                UvmContextFactory.context().hostTable().setHostTableEntry(clientAddress, pusher);
            }
        }

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

    /**
     * Called externally by ipsec-virtual-user-event python script and possibly
     * others when a VPN user disconnects.
     * 
     * @param clientProtocol
     *        The protocol used to connect (L2TP | Xauth | IKEv2)
     * @param clientAddress
     *        The IP address assigned to the client
     * @param clientUsername
     *        The client username
     * @param netRXcount
     *        The total number of bytes received by the client
     * @param netTXcount
     *        The total number of bytes sent by the client
     * @return
     */
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

        event.updateEvent(elapsed, Long.valueOf(netRXcount), Long.valueOf(netTXcount));
        logEvent(event);
        logger.debug("virtualUserGoodbye(logEvent) " + event.toString());

        virtualUserTable.removeVirtualUser(clientAddress, getSettings().getAllowConcurrentLogins());

        updateBlingers();
        return (0);
    }

    /**
     * Called by the UI to forcefully disconnect an active VPN user.
     * 
     * @param clientAddress
     *        The IP address to of the client to disconnected
     * @param clientUsername
     *        The username of the client to be disconnected
     * @return 0 if client was active and disconnected
     */
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

    /**
     * Returns a list of active VPN users.
     * 
     * @return A list of active VPN users
     */
    public LinkedList<VirtualUserEntry> getVirtualUsers()
    {
        logger.debug("getVirtualUsers()");
        return (virtualUserTable.buildUserList());
    }

    /**
     * Returns a list with the status of all enabled IPsec tunnels
     * 
     * @return A list with the status of all enabled IPsec tunnels
     */
    public LinkedList<ConnectionStatusRecord> getTunnelStatus()
    {
        LinkedList<ConnectionStatusRecord> displayList = new LinkedList<>();

        // get the list of configured tunnels from the settings
        LinkedList<IpsecVpnTunnel> configList = settings.getTunnels();
        if (configList == null) return (displayList);

        // create a status display record for all enabled tunnels
        for (int x = 0; x < configList.size(); x++) {
            IpsecVpnTunnel tunnel = configList.get(x);
            if (tunnel.getActive() == false) continue;
            ConnectionStatusRecord record = createDisplayRecord(tunnel);
            displayList.add(record);
        }

        return (displayList);
    }

    /**
     * Creates a UI status display record for an IpsecVpnTunnel
     * 
     * @param tunnel
     *        The tunnel for which status is requested
     * @return The status of the tunnel
     */
    private ConnectionStatusRecord createDisplayRecord(IpsecVpnTunnel tunnel)
    {
        String string;
        long value;
        int top, wid, len;

        ConnectionStatusRecord record = new ConnectionStatusRecord();

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

// THIS IS FOR ECLIPSE - @formatter:off

        /*
         * the script should return the tunnel status in the following format:
         * | TUNNNEL:tunnel_name LOCAL:1.2.3.4 REMOTE:5.6.7.8 STATE:active IN:123 OUT:456 |
         */

// THIS IS FOR ECLIPSE - @formatter:on

        String result = IpsecVpnApp.execManager().execOutput(GRAB_TUNNEL_STATUS_SCRIPT + " " + tunnel.getWorkName());

        /*
         * If the tunnel is active, update the mode and continue parsing.
         * Otherwise just return the inactive record.
         */
        top = result.indexOf("STATE:active");
        if (top > 0) {
            record.setMode("active");
        } else {
            return (record);
        }

        /*
         * We use the IN: and OUT: tags to find the beginning of each value and
         * the trailing space to isolate the numeric portions of the string to
         * keep Long.valueOf happy. We use the LOCAL: and REMOTE: tags to find
         * and display the actual left and right endpoints of active tunnels.
         */

        try {

            top = result.indexOf("IN:");
            wid = 3;
            if (top > 0) {
                len = result.substring(top + wid).indexOf(" ");
                if (len > 0) {
                    value = Long.valueOf(result.substring(top + wid, top + wid + len));
                    record.setInBytes(Long.toString(value));
                }
            }

            top = result.indexOf("OUT:");
            wid = 4;
            if (top > 0) {
                len = result.substring(top + wid).indexOf(" ");
                if (len > 0) {
                    value = Long.valueOf(result.substring(top + wid, top + wid + len));
                    record.setOutBytes(Long.toString(value));
                }
            }

            top = result.indexOf("LOCAL:");
            wid = 6;
            if (top > 0) {
                len = result.substring(top + wid).indexOf(" ");
                if (len > 0) {
                    string = result.substring(top + wid, top + wid + len);
                    if (!string.equals("unknown")) record.setSrc(string);
                }
            }

            top = result.indexOf("REMOTE:");
            wid = 7;
            if (top > 0) {
                len = result.substring(top + wid).indexOf(" ");
                if (len > 0) {
                    string = result.substring(top + wid, top + wid + len);
                    if (!string.equals("unknown")) record.setDst(string);
                }
            }
        }

        /*
         * If we can't parse the tunnel traffic stats just return
         */
        catch (Exception exn) {
            logger.warn("Exception parsing IPsec status: " + result, exn);
        }

        return (record);
    }

    /**
     * This function makes changes to the underlying configuration of the IPsec
     * applications and daemons critical for proper operation.
     * 
     * @throws Exception
     */
    private void fixStrongswanConfig() throws Exception
    {
        /**
         * We add a timeout for the IKE daemon so that commands like up and down
         * don't hang forever, which is the default.
         * 
         * https://lists.strongswan.org/pipermail/users/2013-June/004802.html
         */
        File cfgfile = new File(STRONGSWAN_STROKE_CONFIG);
        StringBuffer buffer = new StringBuffer(1024);
        String line;

        FileReader fileReader = new FileReader(cfgfile);
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(fileReader);

            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains("timeout =") == true) {
                    line = ("    timeout = " + STRONGSWAN_STROKE_TIMEOUT);
                }
                buffer.append(line);
                buffer.append("\n");
            }
            fileReader.close();
        } catch (Exception ex) {
            logger.error("Unable to write to file", ex);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Exception ex) {
                    logger.error("Unable to close file", ex);
                }
            }
        }

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(STRONGSWAN_STROKE_CONFIG);
            fileWriter.write(buffer.toString());
        } catch (Exception ex) {
            logger.error("Unable to write file", ex);
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (Exception ex) {
                    logger.error("Unable to close file", ex);
                }
            }
        }
    }

    /**
     * Callback hook for changes to UVM settings so we know when the certificate
     * assigned to IPsec has been changed.
     *
     * @author mahotz
     *
     */
    private class IpsecVpnHookCallback implements HookCallback
    {

        /**
         * Gets the name for the callback hook
         *
         * @return The name of the callback hook
         */
        public String getName()
        {
            return "ipsecvpn-uvm-settings-change-hook";
        }

        /**
         * Callback handler
         *
         * @param args
         *        The callback arguments
         */
        public void callback(Object... args)
        {
            SystemSettings systemSettings = null;

            Object o = args[0];
            if (!(o instanceof String)) {
                logger.warn("Invalid UVM settings filename: " + o);
                return;
            }

            String fileName = (String) o;

            // we're only interested in changes to the system settings 
            if (!fileName.contains("system.js")) return;

            // load the updated system settings from the argumented file
            try {
                SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
                systemSettings = settingsManager.load(SystemSettings.class, fileName);
            } catch (Exception exn) {
                logger.warn("Unable to read system settings from " + fileName, exn);
                return;
            }

            // if we didn't get the settings just return
            if (systemSettings == null) {
                logger.warn("Invalid system settings file " + fileName);
                return;
            }

            // get the IPsec certificate from the settings
            String adminCertificate = systemSettings.getIpsecCertificate();

            // if the IPsec certificate hasn't changed just return
            if (activeCertificate.equals(adminCertificate)) return;

            // certificate has changed so save the new one and reconfigure
            logger.info("Reconfiguring due to certificate change from " + activeCertificate + " to " + adminCertificate);
            activeCertificate = adminCertificate;
            reconfigure();
        }
    }

    /**
     * Function to register all network address blocks configured in this
     * application
     *
     * @param argSettings
     *        - The application settings
     */
    private void updateNetworkReservations(IpsecVpnSettings argSettings)
    {
        NetspaceManager nsmgr = UvmContextFactory.context().netspaceManager();

        // start by clearing all existing registrations
        nsmgr.clearOwnerRegistrationAll(NETSPACE_OWNER);

        // add registration for L2TP and Xauth address pools if VPN is enabled
        if (argSettings.getVpnflag()) {
            nsmgr.registerNetworkBlock(NETSPACE_OWNER, NETSPACE_L2TP, argSettings.getVirtualAddressPool());
            nsmgr.registerNetworkBlock(NETSPACE_OWNER, NETSPACE_XAUTH, argSettings.getVirtualXauthPool());
        }

        // add registration for the GRE address pool
        nsmgr.registerNetworkBlock(NETSPACE_OWNER, NETSPACE_GRE, argSettings.getVirtualNetworkPool());
    }

    /**
     * Function to check all configured network address blocks for conflicts
     *
     * @param argSettings
     *        - The new application settings
     * @return A string describing the conflict or null if no conflicts are
     *         detected
     */
    private String checkNetworkReservations(IpsecVpnSettings argSettings)
    {
        NetspaceManager nsmgr = UvmContextFactory.context().netspaceManager();
        NetworkSpace space = null;

        IPMaskedAddress lNet = new IPMaskedAddress(argSettings.getVirtualAddressPool());
        IPMaskedAddress xNet = new IPMaskedAddress(argSettings.getVirtualXauthPool());
        IPMaskedAddress gNet = new IPMaskedAddress(argSettings.getVirtualNetworkPool());

        // only need to check for L2TP and Xauth conflits if the VPN flag is enabled 
        if (argSettings.getVpnflag()) {
            if (lNet.isIntersecting(xNet)) {
                return new String("Detected conflict between L2TP (" + lNet + ") and Xauth (" + xNet + ") address pools.");
            }

            if (lNet.isIntersecting(gNet)) {
                return new String("Detected conflict between L2TP (" + lNet + ") and GRE (" + gNet + ") address pools.");
            }

            if (xNet.isIntersecting(gNet)) {
                return new String("Detected conflict between Xauth (" + xNet + ") and GRE (" + gNet + ") address pools.");
            }

            // check the L2TP address pool in the registry
            space = nsmgr.isNetworkAvailable(NETSPACE_OWNER, lNet);
            if (space != null) {
                return new String("L2TP Address Pool conflicts with " + space.ownerName + ":" + space.ownerPurpose);
            }

            // check the Xauth address pool in the registry
            space = nsmgr.isNetworkAvailable(NETSPACE_OWNER, xNet);
            if (space != null) {
                return new String("Xauth Address Pool conflicts with " + space.ownerName + ":" + space.ownerPurpose);
            }
        }

        // check the GRE address pool in the registry
        space = nsmgr.isNetworkAvailable(NETSPACE_OWNER, gNet);
        if (space != null) {
            return new String("GRE Address Pool conflicts with " + space.ownerName + ":" + space.ownerPurpose);
        }

        return null;
    }
}
