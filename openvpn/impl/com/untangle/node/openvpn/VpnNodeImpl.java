/*
 * $Id$
 */
package com.untangle.node.openvpn;

import java.io.FileReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.MailSender;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.HostAddress;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.ValidateException;
import com.untangle.uvm.node.Validator;
import com.untangle.uvm.node.EventLogQuery;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.JsonClient;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.util.XMLRPCUtil;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;

public class VpnNodeImpl extends NodeBase implements VpnNode
{
    private static final String SETTINGS_CONVERSION_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/openvpn-convert-settings.py";

    private static final String STAT_PASS = "pass";
    private static final String STAT_BLOCK = "block";
    private static final String STAT_CONNECT = "connect";

    private static final String NODE_NAME    = "openvpn";
    private static final String WEB_APP      = NODE_NAME;
    private static final String WEB_APP_PATH = "/" + WEB_APP;

    private static final String CLEANUP_SCRIPT = Constants.SCRIPT_DIR + "/cleanup";

    private static final HostAddress EMPTY_HOST_ADDRESS = new HostAddress( "" );

    /* 5 minutes in nanoseconds */
    private static final long DISTRIBUTION_CACHE_NS = TimeUnit.SECONDS.toNanos( 60 * 5 );

    /* Expire these links after an hour */
    private static final long ADMIN_DOWNLOAD_CLIENT_TIMEOUT = TimeUnit.SECONDS.toMillis( 60 * 60 );

    private final Logger logger = Logger.getLogger( VpnNodeImpl.class );

    private boolean isWebAppDeployed = false;

    private final SoloPipeSpec pipeSpec;
    private final SoloPipeSpec[] pipeSpecs;

    private final Random random = new Random();

    private final OpenVpnManager openVpnManager = new OpenVpnManager();
    private final CertificateManager certificateManager = new CertificateManager();
    private final AddressMapper addressMapper = new AddressMapper();
    private final OpenVpnMonitor openVpnMonitor;
    private final OpenVpnCaretaker openVpnCaretaker = new OpenVpnCaretaker();

    private final EventHandler handler;

    VpnSettings settings;

    private Sandbox sandbox = null;

    private String adminDownloadClientKey = null;
    private long   adminDownloadClientExpiration = 0l;

    private final Map <String,DistributionCache> distributionMap = new HashMap<String,DistributionCache>();

    private EventLogQuery connectEventsQuery;

    public VpnNodeImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        this.handler          = new EventHandler( this );
        this.openVpnMonitor   = new OpenVpnMonitor( this );

        /* Have to figure out pipeline ordering, this should always
         * next to towards the outside, then there is OpenVpn and then Nat */
        this.pipeSpec = new SoloPipeSpec( NODE_NAME, this, handler, Fitting.OCTET_STREAM, Affinity.CLIENT, SoloPipeSpec.MAX_STRENGTH - 2);
        this.pipeSpecs = new SoloPipeSpec[] { pipeSpec };

        this.addStat(new NodeMetric(STAT_BLOCK, I18nUtil.marktr("Sessions blocked")));
        this.addStat(new NodeMetric(STAT_PASS, I18nUtil.marktr("Sessions passed")));
        this.addStat(new NodeMetric(STAT_CONNECT, I18nUtil.marktr("Clients Connected")));

        this.connectEventsQuery = new EventLogQuery(I18nUtil.marktr("Closed Sessions"), "FROM OpenvpnLogEventFromReports evt ORDER BY evt.timeStamp DESC");
    }

    private void readNodeSettings()
    {
        SettingsManager setman = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsName = System.getProperty("uvm.settings.dir") + "/untangle-node-openvpn/settings_" + nodeID;
        String settingsFile = settingsName + ".js";
        VpnSettings readSettings = null;

        logger.info("Loading settings from " + settingsFile);

        try {
            readSettings =  setman.load( VpnSettings.class, settingsName);
        }

        catch (Exception exn) {
            logger.error("Could not read node settings", exn);
        }

        // if no settings found try getting them from the database
        if (readSettings == null) {
            logger.warn("No json settings found... attempting to import from database");

            try {
                String convertCmd = SETTINGS_CONVERSION_SCRIPT + " " + nodeID.toString() + " " + settingsFile;
                logger.warn("Running: " + convertCmd);
                UvmContextFactory.context().execManager().exec( convertCmd );
            }

            catch (Exception exn) {
                logger.error("Conversion script failed", exn);
            }

            try {
                readSettings = setman.load( VpnSettings.class, settingsName);
            }

            catch (Exception exn) {
                logger.error("Could not read node settings", exn);
            }

            if (readSettings != null) logger.warn("Database settings successfully imported");
        }

        try {
            if (readSettings == null) {
                logger.warn("No database or json settings found... initializing with defaults");
                initializeSettings();
                writeNodeSettings(getSettings());
            }
            else {
                setSettings(readSettings);
            }
        }
        catch (Exception exn) {
            logger.error("Could not apply node settings", exn);
        }
    }

    private void writeNodeSettings(VpnSettings argSettings)
    {
        SettingsManager setman = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsName = System.getProperty("uvm.settings.dir") + "/untangle-node-openvpn/settings_" + nodeID;

        try {
            setman.save( VpnSettings.class, settingsName, argSettings);
        }

        catch (Exception exn) {
            logger.error("Could not save OpenVPN settings", exn);
        }
    }

    @Override public void initializeSettings()
    {
        VpnSettings settings = new VpnSettings();
        logger.info( "Initializing Settings... to unconfigured" );

        try {
            setSettings( settings );
        } catch ( ValidateException e ) {
            logger.error( "Unable to initialize VPN settings.", e );
        }

        /* Stop the open vpn monitor(I don't think this actually does anything, rbs) */
        this.openVpnMonitor.stop();
        this.openVpnCaretaker.stop();
    }

    // VpnNode methods --------------------------------------------------
    public void setSettings( final VpnSettings newSettings ) throws ValidateException
    {
        /* Verify that all of the client names are valid. */
        for ( VpnClient client : newSettings.trans_getCompleteClientList()) {
            VpnClient.validateName( client.getName());
        }

        /* Attempt to assign all of the clients addresses only if in server mode */
        try {
            if ( !newSettings.isUntanglePlatformClient() && newSettings.trans_isConfigured()) {
                addressMapper.assignAddresses( newSettings );
            }
        } catch ( Exception exn ) {
            logger.warn( "Could not assign client addresses, continuing", exn );
        }

        if ( !newSettings.isUntanglePlatformClient()) {
            /* Update the status/generate all of the certificates for clients */
            this.certificateManager.updateCertificateStatus( newSettings );
        }

        /* Copy in the old keys that were distributed over email */
        saveDistributedKeys(newSettings);
        writeNodeSettings(newSettings);
        this.settings = newSettings;

        try {
            if ( getRunState() == NodeSettings.NodeState.RUNNING ) {
                /* This stops then starts openvpn */
                this.openVpnManager.configure( this.settings );
                this.openVpnManager.restart( this.settings );
                this.handler.configure( this.settings );

                if ( this.settings.isUntanglePlatformClient()) {
                    this.openVpnMonitor.disable();
                    this.openVpnCaretaker.start();
                } else {
                    this.openVpnMonitor.enable();
                }

                /* Make an asynchronous request */
                UvmContextFactory.context().newThread( new GenerateRules( null )).start();
            }

        } catch ( Exception exn ) {
            logger.error( "Could not save VPN settings", exn );
        }
    }

    private void saveDistributedKeys(VpnSettings newSettings)
    {
        if ( this.settings == null ) return;

        /* If it is the same object, there is nothing to do */
        if ( this.settings == newSettings ) return;

        Map<String,String> clientMap = new HashMap<String,String>();
        for ( VpnClient client : this.settings.trans_getCompleteClientList()) {
            clientMap.put( client.trans_getInternalName(), client.getDistributionKey());
        }

        for ( VpnClient client : newSettings.trans_getCompleteClientList()) {
            String key = client.getDistributionKey();
            /* If the key is in the settings object, then do not replace it with what is on the box.
             * (bulk update) */
            if ( key != null && key.length() > 0 ) {
                continue;
            }

            key = clientMap.get(client.trans_getInternalName());

            /* If the previous settings do not have a value, then do not use it. */
            if ( key == null || key.length() == 0 ) {
                continue;
            }

            client.setDistributionKey(key);
        }
    }

    public VpnSettings getSettings()
    {
        /* XXXXXXXXXXXXXXXXXXXX This is not really legit, done so the schema doesn't have to be
         * written for a little while */
        if ( this.settings == null ) this.settings = new VpnSettings();

        return this.settings;
    }

    public VpnClient generateClientCertificate( VpnSettings settings, VpnClient client )
    {
        try {
            certificateManager.createClient( client );
        } catch ( Exception e ) {
            logger.error( "Unable to create a client certificate", e );
        }

        return client;
    }

    public VpnClient revokeClientCertificate( VpnSettings settings, VpnClient client )
    {
        try {
            certificateManager.revokeClient( client );
        } catch ( Exception e ) {
            logger.error( "Unable to revoke a client certificate", e );
        }

        return client;
    }

    private void distributeAllClientFiles( VpnSettings settings ) throws Exception
    {
        for ( VpnClient client : settings.trans_getCompleteClientList()) {
            if ( !client.trans_getDistributeClient()) continue;
            distributeRealClientConfig( client );
        }
    }

    public void distributeClientConfig( VpnClient client )
        throws Exception
    {
        /* Retrieve the client configuration object from the settings */
        boolean foundRealClient = false;
        for ( VpnClient realClient : settings.trans_getCompleteClientList()) {
            if ( client.trans_getInternalName().equals( realClient.trans_getInternalName())) {
                realClient.trans_setDistributionEmail( client.trans_getDistributionEmail());
                client = realClient;
                foundRealClient = true;
                break;
            }
        }

        if ( foundRealClient ) distributeRealClientConfig( client );
        else throw new Exception( "Attempt to distribute an unsaved client" );
    }

    /** The client config is the same client configuration object that is in settings */
    private void distributeRealClientConfig( final VpnClient client )
        throws Exception
    {
        /* this client may already have a key, the key may have
         * already been created. */

        this.certificateManager.createClient( client );

        final String email = client.trans_getDistributionEmail();
        String method = "download";

        if ( email != null ) {
            /* Generate a random key for email distribution */
            String key = client.getDistributionKey();

            /* Only generate a new key if the key has been modified */
            if ( key == null || key.length() == 0 ) {
                if ( client.trans_isUntanglePlatform()) {
                    /* Use a shorter key for edge guard clients since they have to be
                     * typed in manually */
                    key = String.format( "%08x", random.nextInt());
                } else {
                    key = String.format( "%08x%08x", random.nextInt(), random.nextInt());
                }
            }
            client.setDistributionKey( key );

            method = "email";
        }

        this.openVpnManager.writeClientConfigurationFiles( settings, client, method );

        if ( email != null ) distributeClientConfigEmail( client, email );
    }

    private void distributeClientConfigEmail( VpnClient client, String email )
        throws Exception
    {
        try {
            UvmContext uvm = UvmContextFactory.context();
            MailSender mailSender = uvm.mailSender();
            Map<String,String> i18nMap = uvm.languageManager().getTranslations("untangle-node-openvpn");
            String subject = I18nUtil.tr("OpenVPN Client", i18nMap);

            /* Read in the contents of the file */
            FileReader fileReader = new FileReader( Constants.PACKAGES_DIR + "/mail-" +
                                                    client.trans_getInternalName() + ".eml" );
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1024];
            int rs;
            while (( rs = fileReader.read( buf )) > 0 ) sb.append( buf, 0, rs );

            try {
                fileReader.close();
            } catch ( IOException e ) {
                logger.warn( "Unable to close file", e );
            }

            String recipients[] = { email };

            mailSender.sendHtmlMessage( recipients, subject, sb.toString());
        } catch ( Exception e ) {
            logger.warn( "Error distributing client key", e );
            throw new Exception( e );
        }
    }

    /* Get the common name for the key, and clear it if it exists */
    public synchronized String lookupClientDistributionKey( String key, IPAddress clientAddress )
    {
        if (logger.isDebugEnabled()) {
            logger.debug( "Looking up client for key: " + key );
        }

        long requestTime = System.nanoTime();

        /* cleanup the hash for expired distribution caches. */
        for ( Iterator<String> i = this.distributionMap.keySet().iterator() ; i.hasNext() ; ) {
            DistributionCache dc = this.distributionMap.get( i.next());
            if ( dc == null ) {
                /* not really possible... */
                continue;
            }
            if ( requestTime > dc.getExpirationTime()) {
                i.remove();
            }
        }

        /* First check if it is in the hash */
        DistributionCache dc = this.distributionMap.get( key );
        if ( dc != null ) {
            return dc.getCommonName();
        }

        /* Could use a hash map, but why bother ? */
        for ( final VpnClient client : this.settings.trans_getCompleteClientList()) {
            if ( lookupClientDistributionKey( key, clientAddress, client )) return client.trans_getInternalName();
        }

        return null;
    }

    /* Returns a URL to use to download the admin key. */
    public String getAdminDownloadLink( String clientName, ConfigFormat format )
        throws Exception
    {
        boolean foundClient = false;
        for ( final VpnClient client : this.settings.trans_getCompleteClientList()) {
            if ( !client.trans_getInternalName().equals( clientName ) &&
                 !client.getName().equals( clientName )) continue;

            clientName = client.trans_getInternalName();

            /* Clear out the distribution email */
            client.trans_setDistributionEmail( null );
            distributeRealClientConfig( client );
            foundClient = true;
            break;
        }

        if ( !foundClient ) {
            throw new Exception( "Unable to download unsaved clients <" + clientName +">" );
        }

        generateAdminClientKey();

        String fileName = null;
        switch ( format ) {
        case SETUP_EXE : fileName = "setup.exe";  break;
        case ZIP: fileName = "config.zip"; break;
        }

        String key = "";
        String client = "";
        try {
            key = URLEncoder.encode( this.adminDownloadClientKey , "UTF-8");
            client = URLEncoder.encode( clientName , "UTF-8");
        } catch(java.io.UnsupportedEncodingException e) {
            logger.warn("Unsupported Encoding:",e);
        }

        return WEB_APP_PATH + "/" + fileName +
            "?" + Constants.ADMIN_DOWNLOAD_CLIENT_KEY + "=" + key +
            "&" + Constants.ADMIN_DOWNLOAD_CLIENT_PARAM + "=" + client;
    }

    /* Returns true if this is the correct authentication key for
     * downloading keys as the administrator */
    public boolean isAdminKey( String key )
    {
        long now = System.currentTimeMillis();

        /* This is designed to protect against clock changes and keys create way in the future */
        if (( this.adminDownloadClientExpiration < now ) ||
            ( this.adminDownloadClientExpiration > ( now + ( ADMIN_DOWNLOAD_CLIENT_TIMEOUT * 2 )))) {
            this.adminDownloadClientExpiration = 0;
            this.adminDownloadClientKey = null;
        }

        if (( this.adminDownloadClientKey == null ) || !this.adminDownloadClientKey.equals( key )) {
            return false;
        }

        return true;
    }

    private boolean lookupClientDistributionKey( String key, IPAddress clientAddress, final VpnClient client )
    {
        String clientKey = client.getDistributionKey();

        /* XXX, possible check if it is live ??? */

        if ( clientKey != null ) clientKey = clientKey.trim();
        if (logger.isDebugEnabled()) {
            logger.debug( "Checking: " + clientKey );
        }

        if ( clientKey == null || clientKey.length() == 0 ) return false;
        if ( !clientKey.equalsIgnoreCase( key )) return false;

        client.setDistributionKey( null );

        long expiration = System.nanoTime() + DISTRIBUTION_CACHE_NS;
        this.distributionMap.put( key, new DistributionCache( expiration, client.trans_getInternalName(), key ));

        return true;
    }

    private synchronized void deployWebApp()
    {
        if ( !isWebAppDeployed ) {
            if (null != UvmContextFactory.context().localAppServerManager().loadInsecureApp( WEB_APP_PATH, WEB_APP)) {
                logger.debug( "Deployed openvpn web app" );
            }
            else logger.warn( "Unable to deploy openvpn web app" );
        }
        isWebAppDeployed = true;

        /* Make sure to leave this in because it reloads the iptables rules. */
        UvmContextFactory.context().networkManager().refreshIptablesRules();
    }

    private synchronized void unDeployWebApp()
    {
        if ( isWebAppDeployed ) {
            if( UvmContextFactory.context().localAppServerManager().unloadWebApp(WEB_APP_PATH )) {
                logger.debug( "Unloaded openvpn web app" );
            } else logger.warn( "Unable to unload openvpn web app" );
        }
        isWebAppDeployed = false;
    }

    // NodeBase methods ----------------------------------------------

    @Override protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // lifecycle --------------------------------------------------------------
    @Override protected void preInit()
    {
        super.preInit();

        try {
            this.openVpnMonitor.start();
        } catch ( Exception e ) {
            logger.warn( "Unable to start openvpn monitor." );
        }
    }

    @Override protected void postInit()
    {
        super.postInit();
        readNodeSettings();
        deployWebApp();
    }

    @Override protected void preStart()
    {
        super.preStart();

        Map<String,String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle-node-openvpn");
        I18nUtil i18nUtil = new I18nUtil(i18nMap);

        if ( this.settings == null ) {
            postInit();

            if ( this.settings == null ) initializeSettings();
        }

        /* Don't start if openvpn cannot be configured */
        if ( !settings.trans_isConfigured()) {
            throw new RuntimeException( i18nUtil.tr( "You must configure OpenVPN as either a VPN Routing Server or a VPN Client before you can turn it on.  You may do this through its Setup Wizard (in its settings)." ));
        }

        try {
            settings.validate();

            this.openVpnManager.configure( settings );
            this.handler.configure( settings );
            this.openVpnManager.restart( settings );

        } catch( Exception e ) {
            try {
                this.openVpnManager.stop();
            } catch ( Exception stopException ) {
                logger.error( "Unable to stop the openvpn process", stopException );
            }
            throw new RuntimeException(e);
        }

        deployWebApp();

        /* Only start the monitor for servers */
        if ( settings.isUntanglePlatformClient()) {
            this.openVpnMonitor.disable();
            this.openVpnCaretaker.start();
        } else {
            this.openVpnMonitor.enable();
        }
    }

    @Override protected void postStop()
    {
        super.postStop();
    }

    @Override protected void preStop()
    {
        super.preStop();

        try {
            this.openVpnMonitor.disable();
            this.openVpnCaretaker.stop();
        } catch ( Exception e ) {
            logger.warn( "Error disabling openvpn monitor", e );
        }

        try {
            this.openVpnManager.stop();
        } catch ( Exception e ) {
            logger.warn( "Error stopping openvpn manager", e );
        }

        /* unregister the service with the UVM */
        /* Make sure to leave this in because it reloads the iptables rules. */
        //UvmContextFactory.context().networkManager().unregisterService( SERVICE_NAME );
        UvmContextFactory.context().networkManager().refreshIptablesRules();
    }

    @Override protected void postDestroy()
    {
        super.postDestroy();

        try {
            this.openVpnMonitor.stop();
            this.openVpnCaretaker.stop();
        } catch ( Exception e ) {
            logger.warn( "Error stopping openvpn monitor", e );
        }

        unDeployWebApp();
    }

    @Override protected void uninstall()
    {
        super.uninstall();

        unDeployWebApp();

//         try {
//             UvmContextFactory.context().localIntfManager().unregisterIntf( IntfConstants.VPN_INTF );
//         } catch ( Exception e ) {
//             /* There is nothing else to do but print out the message */
//             logger.error( "Unable to deregister vpn interface", e );
//         }

        try {
            UvmContextFactory.context().execManager().exec( CLEANUP_SCRIPT );
        } catch ( Exception e ) {
            logger.error( "Unable to cleanup data.", e );
        }
    }

    public ConfigState getConfigState()
    {
        if ( settings == null || !settings.trans_isConfigured()) return ConfigState.UNCONFIGURED;

        if ( settings.isUntanglePlatformClient()) return ConfigState.CLIENT;

        return ( settings.isBridgeMode() ? ConfigState.SERVER_BRIDGE : ConfigState.SERVER_ROUTE );
    }

    public HostAddress getVpnServerAddress()
    {
        /* Return the empty address */
        if (( this.settings == null ) || ( getConfigState() != ConfigState.CLIENT )) {
            logger.info( "non-client state, and request for server address" );
            return EMPTY_HOST_ADDRESS;
        }

        HostAddress address = this.settings.getServerAddress();

        if ( address == null ) {
            logger.info( "null, host address, returning empty address." );
            return EMPTY_HOST_ADDRESS;
        }

        return address;
    }

    public void startConfig( ConfigState state ) throws ValidateException
    {
        if ( state == ConfigState.UNCONFIGURED || state == ConfigState.SERVER_BRIDGE ) {
            throw new ValidateException( "Cannot run wizard for the selected state: " + state );
        }

        this.sandbox = new Sandbox( state );

        if ( state == ConfigState.SERVER_ROUTE ) {
            this.sandbox.autoDetectAddressPool();
            this.sandbox.autoDetectExportList();
        }
    }

    public String getAdminClientUploadLink()
    {
        generateAdminClientKey();

        String key = "";
        try {
            key = URLEncoder.encode( this.adminDownloadClientKey , "UTF-8");
        } catch(java.io.UnsupportedEncodingException e) {
            logger.warn("Unsupported Encoding:",e);
        }

        return WEB_APP_PATH + "/clientSetup?" + Constants.ADMIN_DOWNLOAD_CLIENT_KEY + "=" + key;
    }

    public void completeConfig() throws Exception
    {
        VpnSettings newSettings = this.sandbox.completeConfig( this.getNodeSettings());

        /* Generate new settings */
        if ( newSettings.isUntanglePlatformClient()) {
            /* Finish the configuration for clients, nothing left to do,
             * it is all done at download time */
        } else {
            /* Try to cleanup the previous data */
            UvmContextFactory.context().execManager().exec( CLEANUP_SCRIPT );

            /* Create the base certificate and parameters */
            this.certificateManager.createBase( newSettings );

            /* Create clients certificates */
            this.certificateManager.createAllClientCertificates( newSettings );

            /* Distribute emails */
            distributeAllClientFiles( newSettings );
        }

        setSettings( newSettings );

        /* No reusing the sanbox */
        this.sandbox = null;
    }

    public void installClientConfig( String path ) throws Exception
    {
        this.sandbox.installClientConfig( path );
    }

    public void generateCertificate( CertificateParameters parameters ) throws Exception
    {
        this.sandbox.generateCertificate( parameters );
    }

    public GroupList getAddressGroups() throws Exception
    {
        return this.sandbox.getGroupList();
    }

    public void setAddressGroups( GroupList parameters ) throws Exception
    {
        this.sandbox.setGroupList( parameters );
    }

    public ExportList getExportedAddressList()
    {
        return this.sandbox.getExportList();
    }

    public void setExportedAddressList( ExportList parameters ) throws Exception
    {
        this.sandbox.setExportList( parameters );
    }

    public void setClients( ClientList parameters) throws Exception
    {
        this.sandbox.setClientList( parameters );
    }

    public void setSites( SiteList parameters) throws Exception
    {
        this.sandbox.setSiteList( parameters );
    }

    public List<ClientConnectEvent> getActiveClients()
    {
        return this.openVpnMonitor.getOpenConnectionsAsEvents();
    }

    public EventLogQuery[] getConnectEventsQueries()
    {
        return new EventLogQuery[] { this.connectEventsQuery };
    }

    public void incrementBlockCount()
    {
        this.incrementStat(this.STAT_BLOCK);
    }

    public void incrementPassCount()
    {
        this.incrementStat(this.STAT_PASS);
    }

    public void incrementConnectCount()
    {
        this.incrementStat(this.STAT_CONNECT);
    }

    public Validator getValidator()
    {
        return new OpenVpnValidator();
    }

    private void generateAdminClientKey()
    {
        long now = System.currentTimeMillis();

        /* This is designed to protect against clock changes and keys create way in the future */
        if (( this.adminDownloadClientExpiration < now ) ||
            ( this.adminDownloadClientExpiration > ( now + ( ADMIN_DOWNLOAD_CLIENT_TIMEOUT * 2 )))) {
            this.adminDownloadClientExpiration = 0;
            this.adminDownloadClientKey = null;
        }

        if ( this.adminDownloadClientKey == null ) {
            this.adminDownloadClientKey = String.format( "%08x%08x",
                                                         this.random.nextInt(), this.random.nextInt());
            this.adminDownloadClientExpiration = now + ADMIN_DOWNLOAD_CLIENT_TIMEOUT;
        }
    }

    class GenerateRules implements Runnable
    {
        private final Runnable callback;

        public GenerateRules( Runnable callback )
        {
            this.callback = callback;
        }

        public void run()
        {
            try {
                JsonClient.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, "generate_rules", null );
            } catch ( Exception e ) {
                logger.error( "Error while generating iptables rules", e );
            }

            if ( this.callback != null ) this.callback.run();
        }
    }

    class DistributionCache
    {
        /* This is the request time using nanoTime which is independent of clock time */
        private final long expirationTime;
        private final String commonName;
        private final String key;

        DistributionCache( long expirationTime, String commonName, String key )
        {
            this.expirationTime = expirationTime;
            this.commonName = commonName;
            this.key = key;
        }

        public long getExpirationTime()
        {
            return this.expirationTime;
        }

        public String getCommonName()
        {
            return this.commonName;
        }

        public String getKey()
        {
            return this.key;
        }
    }
}
