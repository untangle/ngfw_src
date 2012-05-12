/**
 * $Id$
 */
package com.untangle.uvm.networking;

import static com.untangle.uvm.networking.ShellFlags.FILE_RULE_CFG;

import java.net.ConnectException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.io.File;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.jnetcap.Netcap;
import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmState;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.node.SessionTuple;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.ValidateException;
import com.untangle.uvm.node.IPMatcher;
import com.untangle.uvm.node.ScriptWriter;
import com.untangle.uvm.util.JsonClient;
import com.untangle.uvm.util.XMLRPCUtil;
import com.untangle.uvm.SettingsManager;

/**
 * The Network Manager handles all the network configuration
 */
public class NetworkManagerImpl implements NetworkManager
{
    private static NetworkManagerImpl INSTANCE = null;

    static final String ETC_INTERFACES_FILE = "/etc/network/interfaces";
    static final String ETC_RESOLV_FILE = "/etc/resolv.conf";

    private final Logger logger = Logger.getLogger(getClass());

    static final String UVM_BASE = System.getProperty( "uvm.home" );
    static final String UVM_CONF = System.getProperty( "uvm.conf.dir" );

    static final String ALPACA_SCRIPT = "/usr/share/untangle-net-alpaca/scripts/";

    private static final long ALPACA_RETRY_COUNT = 3;
    private static final long ALPACA_RETRY_DELAY_MS = 6000;

    /* Inidicates whether or not the networking manager has been initialized */
    private boolean isInitialized = false;

    /* Manager for the iptables rules */
    private final RuleManager ruleManager;

    /* Manager for AccessSettings */
    private final AccessManagerImpl accessManager;

    /* Manager for AddressSettings */
    private final AddressManagerImpl addressManager;

    /* networkListeners stores parties interested in being notified of network changes */
    private Set<NetworkConfigurationListener> networkListeners = new HashSet<NetworkConfigurationListener>();

    /** The nuts and bolts of networking, the real bits of panther.  this my friend
     * should never be null */
    private NetworkConfiguration networkConfiguration = null;

    /* Flag to indicate when the UVM has been shutdown */
    private boolean isShutdown = false;

    private NetworkManagerImpl()
    {
        this.ruleManager = RuleManager.getInstance();
        this.accessManager = new AccessManagerImpl();
        this.addressManager = new AddressManagerImpl();
    }

    /**
     * The init function cannot fail, if it does, reasonable defaults
     * must be used, so if initPriv fails(which is why it throws
     * Exception), then this function grabs reasonable defaults and
     * moves on
     */
    public synchronized void init()
    {
        if ( isInitialized ) {
            logger.error( "Attempt to reinitialize the networking manager", new Exception());
            return;
        }

        try {
            initPriv();
        } catch ( Exception e ) {
            logger.error( "Exception initializing settings, using reasonable defaults", e );

            /* !!!!!!!! use reasonable defaults */
        }

        this.isInitialized = true;
    }

    public void updateLinkStatus()
    {
        InterfaceTester.getInstance().updateLinkStatus( this.networkConfiguration );
    }

    /* Return the primary address of the device, this is the primary
     * external address.  which is the first address registered on the
     * first network space */
    public IPAddress getPrimaryAddress()
    {
        NetworkConfiguration settings = this.networkConfiguration;

        if ( settings == null )
            return null;

        InterfaceConfiguration wan = settings.findFirstWAN();

        if ( wan == null )
            return null;

        IPNetwork primary = wan.getPrimaryAddress();

        if ( primary == null) {
            logger.warn("NULL primary address for wan: " + wan.getName());
            return null;
        }
        
        return primary.getNetwork();
    }

    /**
     * Retrieve the settings related to limiting access to the box.
     */
    @Override
    public AccessSettings getAccessSettings()
    {
        return this.accessManager.getSettings();
    }

    @Override
    public void setAccessSettings( AccessSettings access )
    {
        this.accessManager.setSettings( access );

        refreshNetworkConfig();

        try {
            generateRules();
        } catch ( Exception e ) {
            logger.warn( "Unable to generate rules.", e );
        }
    }

    /**
     * Retrieve the settings related to the hostname and the address used to access to the box.
     */
    @Override
    public AddressSettings getAddressSettings()
    {
        return this.addressManager.getSettings();
    }

    @Override
    public void setAddressSettings( AddressSettings address )
    {
        this.addressManager.setSettings( address );

        refreshNetworkConfig();

        try {
            generateRules();
        } catch ( Exception e ) {
            logger.warn( "Unable to generate rules.", e );
        }
    }

    /* Register a service that needs outside access to HTTPs, the name should be unique */
    public synchronized void registerService( String name )
    {
        this.accessManager.registerService( name );

        try {
            generateRules();
        } catch ( Exception e ) {
            logger.error( "Unable to create rules", e );
        }
    }

    /* Remove a service that needs outside access to HTTPs, the name should be unique */
    public synchronized void unregisterService( String name )
    {
        this.accessManager.unregisterService( name );

        try {
            generateRules();
        } catch ( Exception e ) {
            logger.error( "Unable to create rules", e );
        }
    }

    public NetworkConfiguration getNetworkConfiguration()
    {
        return this.networkConfiguration;
    }

    public void remapInterfaces( String[] osArray, String[] userArray ) throws Exception
    {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put( "os_names", new JSONArray( osArray ));
            jsonObject.put( "user_names", new JSONArray( userArray ));
        } catch ( JSONException e ) {
            throw new Exception( "Unable to build JSON Object", e );
        }

        try {
            JsonClient.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, "remap_interfaces", jsonObject );
        } catch ( Exception e ) {
            throw new Exception( "Unable to configure the external interface.", e );
        }
    }

    /* Set the access and address settings, used by the Remote Panel */
    public void setSettings( AccessSettings access, AddressSettings address )
        throws Exception, ValidateException
    {
        this.accessManager.setSettings( access );
        this.addressManager.setSettings( address );

        refreshNetworkConfig();

        generateRules();
    }

    /* Set the Access, Misc and Network settings at once.  Used by the
     * support panel */
    public void setSettings( AccessSettings access )
        throws Exception, ValidateException
    {
        this.accessManager.setSettings( access );

        refreshNetworkConfig();

        generateRules();
    }

    /* Get the current hostname */
    public String getHostname()
    {
        return this.networkConfiguration.getHostname();
    }

    public String getPublicAddress()
    {
        String publicAddr = this.addressManager.getSettings().getCurrentURL();

        //try never to return null
        if (publicAddr == null) {
            publicAddr = this.networkConfiguration.getHostname();
        }
        if (publicAddr == null) {
            logger.warn("Unable to determine public address\n");
        }

        return publicAddr;
    }

    /* Get the external HTTPS port */
    public int getPublicHttpsPort()
    {
        return this.addressManager.getSettings().getPublicPort();
    }

    /* Save the network settings during the wizard */
    public void setSetupSettings( AddressSettings address, InterfaceConfiguration settings )
        throws Exception, ValidateException
    {
        this.addressManager.setWizardSettings( address );

        setSetupSettings( settings );
    }

    public InterfaceConfiguration setSetupSettings( InterfaceConfiguration wan )
        throws Exception, ValidateException
    {
        /* Send the call onto the alpaca */

        JSONObject jsonObject = new JSONObject();
        String method = null;
        
        try {
            if ( InterfaceConfiguration.CONFIG_PPPOE.equals(wan.getConfigType()) ) {
                /* PPPoE Setup */
                method = "wizard_external_interface_pppoe";
                jsonObject.put( "username", wan.getPPPoEUsername());
                jsonObject.put( "password", wan.getPPPoEPassword());
            } else if ( InterfaceConfiguration.CONFIG_DYNAMIC.equals(wan.getConfigType()) ) {
                /* Dynamic address */
                method = "wizard_external_interface_dynamic";
            } else if ( InterfaceConfiguration.CONFIG_STATIC.equals(wan.getConfigType()) ) {
                /* Must be a static address */
                jsonObject.put( "ip", wan.getPrimaryAddress().getNetwork().toString());
                jsonObject.put( "netmask", wan.getPrimaryAddress().getNetmask().toString());
                jsonObject.put( "default_gateway", wan.getGateway().toString().replace("/",""));
                jsonObject.put( "dns_1", wan.getDns1().toString().replace("/",""));
                InetAddress dns2 = wan.getDns2();
                if ( dns2 != null ) jsonObject.put( "dns_2", dns2.toString());
                method = "wizard_external_interface_static";
            } else {
                throw new Exception( "Unknown config type: " + wan.getConfigType());
            }
        } catch ( JSONException e ) {
            throw new Exception( "Unable to build JSON Object", e );
        }

        /* Make a synchronous request */
        Exception e = retryAlpacaCall( method, jsonObject );
        if ( e != null ) {
            logger.warn( "Unable to configure the external interface.", e );
            throw new Exception( "Unable to configure the external interface.", e );
        }

        /**
         * now force a write of netConfig.js
         * and re-read it
         */
        e = retryAlpacaCall( "write_files", null );
        if ( e != null ) {
            logger.warn( "Unable to write files.", e );
        }
        refreshNetworkConfig();

        return getNetworkConfiguration().findFirstWAN();
    }

    /**
     * Returns the first WAN
     */
    public InterfaceConfiguration getWizardWAN()
    {
        return getNetworkConfiguration().findFirstWAN();
    }

    /**
     * returns a recommendation for the internal network. 
     * @param externalAddress The external address, if null, this uses the external address of the box.
     */
    public IPNetwork getWizardInternalAddressSuggestion( IPAddress externalAddress )
    {
        try {
            NetworkConfiguration netConf = getNetworkConfiguration();

            if ( netConf == null ) {
                logger.warn("NULL network configuration");
                return IPNetwork.parse( "192.168.1.1/24" );
            }
            
            if ( externalAddress == null ) { 
                externalAddress = netConf.findFirstWAN().getPrimaryAddress().getNetwork();
            }
            
            if ( externalAddress == null ) {
                return IPNetwork.parse( "192.168.1.1/24" );
            }

            /**
             * Use the already existing internal addr by default
             * However, If it overlaps with external - suggest something else
             */
            InterfaceConfiguration internal = netConf.findByName("Internal");
            if (internal != null) {
                IPNetwork internalNet = internal.getPrimaryAddress();

                /**
                 * If it doesn't have a current address just use 192.168.1.1
                 */
                if (internalNet == null)
                    internalNet = IPNetwork.parse( "192.168.1.1/24" );
                    
                IPMatcher internalMatcher = new IPMatcher( internalNet );

                if ( internalMatcher.isMatch( externalAddress.getAddr() ) ) {
                    return IPNetwork.parse( "172.16.0.1/24" );
                }

                return internalNet;
            }
            
            return IPNetwork.parse( "192.168.1.1/24" );
            
        } catch ( Exception e ) {
            /* This should never happen */
            throw new RuntimeException( "Unable to suggest an internal address", e );
        }
    }

    public void setWizardNatEnabled( IPAddress address, IPAddress netmask, boolean enableDhcpServer )
        throws Exception
    {
        logger.debug( "enabling nat as requested by setup wizard: " + address + "/" + netmask );
        logger.debug( "use-dhcp: " + enableDhcpServer );
        
        /* Make a synchronous request */
        JSONObject jsonObject  = new JSONObject();
        try {
            jsonObject.put( "ip", address.toString());
            jsonObject.put( "netmask", netmask.toString());
            jsonObject.put( "is_dhcp_enabled", enableDhcpServer );
        } catch ( JSONException e ) {
            throw new Exception( "Unable to build JSON Object", e );
        }
        Exception e = retryAlpacaCall( "wizard_internal_interface_nat", jsonObject );

        if ( e != null ) {
            logger.warn( "unable to setup system for NAT in wizard.", e );
            throw new Exception( "Unable to enable nat settings.", e );
        }
    }

    public void setWizardNatDisabled() throws Exception
    {
        logger.debug( "disabling nat in setup wizard: " );

        /* Make a synchronous request */
        Exception e = retryAlpacaCall( "wizard_internal_interface_bridge", null );
        if ( e != null ) {
            logger.warn( "Unable to disable NAT in wizard", e );
            throw new Exception( "Unable to disable NAT.", e );
        }
    }

    public Boolean isQosEnabled()
    {
        try {
            JSONObject jsonObject = JsonClient.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, "get_qos_settings", null );

            JSONObject result = jsonObject.getJSONObject("result");
            if (result == null)
                return Boolean.FALSE;

            try {
                Boolean enabled = result.getBoolean("enabled");
                if (enabled == null) {
                    logger.warn("Unable to retrieve QoS settings: null");
                }
                return enabled;
            }
            catch (org.json.JSONException e) {
                //org.json.JSONException: JSONObject["enabled"] is not a Boolean.
                //assume its off
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Unable to retrieve QoS settings:",e);
            return null;
        }
    }
    
    public JSONArray getWANSettings()
    {
        try {
            JSONObject jsonObject = JsonClient.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, "get_wan_interfaces", null );
            JSONArray result = jsonObject.getJSONArray("result");
            return result;
            
        } catch (Exception e) {
            logger.error("Unable to retrieve WAN settings:",e);
            return null;
        }
    }

    public void enableQos()
    {
        try {
            JSONObject jsonObject = JsonClient.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, "enable_qos", null );
            return;
        } catch (Exception e) {
            logger.error("Unable to enable Qos:",e);
            return;
        }
    }

    private void _setWANSpeed(String name, String property, int speed)
    {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name",name);
            jsonObject.put(property,speed);

            jsonObject = JsonClient.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, "set_wan_speed", jsonObject );
            return;
        } catch (Exception e) {
            logger.error("Unable to set WAN settings:",e);
            return;
        }
    }
    
    public void setWANDownloadBandwidth(String name, int speed)
    {
        this._setWANSpeed(name,"download_bandwidth",speed);
    }

    public void setWANUploadBandwidth(String name, int speed)
    {
        this._setWANSpeed(name,"upload_bandwidth",speed);
    }
    
    public void refreshNetworkConfig()
    {
        logger.info("Refreshing Network Configuration...");

        /**
         * If the UVM is shutting down, don't bother doing anything
         */
        if ( UvmContextFactory.context().state() == UvmState.DESTROYED)
            return;
                                 
        this.networkConfiguration = loadNetworkConfiguration( );

        if (this.networkConfiguration == null) {
            logger.error("Failed to load network configuration, creating reasonable default.");
            this.networkConfiguration = new NetworkConfiguration();
        }
        
        if ( logger.isDebugEnabled()) {
            logger.debug( "New network settings: " + this.networkConfiguration );
        }

        /**
         * Write networking.sh
         */
        try {
            ScriptWriter scriptWriter = new ScriptWriter();
            scriptWriter.appendLine("#");
            scriptWriter.appendLine("# This file is written by the untangle-vm and used by various system scripts");
            scriptWriter.appendLine("# to quickly fetch some of the system settings");
            scriptWriter.appendLine("#");
            scriptWriter.appendLine("");

            this.accessManager.commit( scriptWriter );
            this.addressManager.commit( scriptWriter );
            this.ruleManager.commit( scriptWriter );
            /* Save out the script */
            scriptWriter.writeFile( FILE_RULE_CFG );
        } catch ( Exception e ) {
            logger.warn( "Error committing the networking.sh file", e );
        }

        try {
            callNetworkListeners();
        } catch ( Exception e ) {
            logger.error( "Exception in a listener", e );
        }

        logger.info("Refreshed  Network Configuration.");
    }

    public void refreshIptablesRules()
    {
        try {
            this.generateRules();
        } catch (Exception e) {
            logger.warn("Failed to refresh IPtables: ",e);
        }
    }

    public String[] getPossibleInterfaces()
    {
        LinkedList<String> possibleInterfaces = new LinkedList<String>();

        /**
         * These are always possible
         */
        possibleInterfaces.add("any");
        possibleInterfaces.add("wan");
        possibleInterfaces.add("non_wan");

        if (this.networkConfiguration != null) {   
            for ( InterfaceConfiguration intfConf : this.networkConfiguration.getInterfaceList() ) {
                possibleInterfaces.add(intfConf.getInterfaceId().toString());
            }
        }

        return possibleInterfaces.toArray(new String[0]);
    }

    public String[] getWanInterfaces()
    {
        LinkedList<String> wanInterfaces = new LinkedList<String>();

        if (this.networkConfiguration != null) {   
            for ( InterfaceConfiguration intfConf : this.networkConfiguration.getInterfaceList() ) {
                if (intfConf.isWAN())
                    wanInterfaces.add(intfConf.getInterfaceId().toString());
            }
        }

        return wanInterfaces.toArray(new String[0]);
    }
    
    /*
     * This returns an address where the host should be able to access
     * HTTP.  if HTTP is not reachable, this returns NULL
     */
    public InetAddress getInternalHttpAddress( int clientIntf )
    {
        /* Retrieve the network settings */
        NetworkConfiguration netConf = this.networkConfiguration;
        if ( netConf == null ) {
            logger.warn("Failed to fetch network configuration");
            return null;
        }

        InterfaceConfiguration intfConf = netConf.findById( clientIntf );
        if ( intfConf == null ) {
            logger.warn("Failed to fetch interface configuration");
            return null;
        }

        /* WAN ports never have HTTP open */
        boolean isWan = intfConf.isWAN();
        if ( isWan ) {
            //this is normal no error logged
            return null;
        }
        
        /**
         * If this interface is bridged with another, use the addr from the other
         */
        if (InterfaceConfiguration.CONFIG_BRIDGE.equals(intfConf.getConfigType())) {
            String bridgedTo = intfConf.getBridgedTo();
            intfConf = netConf.findByName( bridgedTo );

            if ( intfConf == null ) {
                logger.warn("No Interface found for name: " + bridgedTo );
                return null;
            }
        }

        IPNetwork network = intfConf.getPrimaryAddress();

        if ( network == null ) {
            logger.warn("No primary address for: " + intfConf.getName());
            return null;
        }

        IPAddress address = network.getNetwork();

        if ( address == null ) {
            logger.warn("No address for: " + network);
            return null;
        }

        if ( NetworkUtilPriv.getPrivInstance().isBogus( address ) ) {
            logger.warn("Bogus address: " + address);
            return null;
        }

        return address.getAddr();
    }

    /* Update all of the iptables rules and the inside address database */
    private void generateRules() throws Exception
    {
        if ( this.isShutdown ) return;

        ScriptWriter scriptWriter = new ScriptWriter();
        scriptWriter.appendLine("#");
        scriptWriter.appendLine("# This file is written by the untangle-vm and used by various system scripts");
        scriptWriter.appendLine("# to quickly fetch some of the system settings");
        scriptWriter.appendLine("#");
        scriptWriter.appendLine("");

        this.accessManager.commit( scriptWriter );
        this.addressManager.commit( scriptWriter );
        this.ruleManager.commit( scriptWriter );

        /* Save out the script */
        scriptWriter.writeFile( FILE_RULE_CFG );

        this.ruleManager.generateIptablesRules();
    }

    public void isShutdown()
    {
        this.isShutdown = true;
        this.ruleManager.isShutdown();
    }

    public void flushIPTables() throws Exception
    {
        this.ruleManager.destroyIptablesRules();
    }

    /* Listener functions */
    private void callNetworkListeners()
    {
        logger.debug( "Calling network listeners." );
        for ( NetworkConfigurationListener listener : this.networkListeners ) {
            if ( logger.isDebugEnabled()) logger.debug( "Calling listener: " + listener );

            try {
                listener.event( this.networkConfiguration );
            } catch ( Exception e ) {
                logger.error( "Exception calling listener", e );
            }
        }
        logger.debug( "Done calling network listeners." );
    }

    public void registerListener( NetworkConfigurationListener networkListener )
    {
        this.networkListeners.add( networkListener );
    }

    public void unregisterListener( NetworkConfigurationListener networkListener )
    {
        this.networkListeners.remove( networkListener );
    }

    /* ----------------- Package ----------------- */

    /* ----------------- Private ----------------- */
    private void initPriv() throws Exception, ValidateException
    {
        loadAllSettings();

        refreshNetworkConfig();

        try {
            generateRules();
        } catch ( Exception e ) {
            logger.error( "Exception generating rules", e );
        }

        /* Update the link status for all of the interfaces */
        updateLinkStatus();
    }

    /* Methods for saving and loading the settings files from the database at startup */
    private void loadAllSettings()
    {
        /* Load the access settings. */
        this.accessManager.init();

        /* Load the address/hostname settings */
        this.addressManager.init();
    }

    /* Retry a call to the alpaca in case it was restarted. */
    private Exception retryAlpacaCall( String method, JSONObject jsonObject )
    {
        /* Make a synchronous request */
        for ( int c = 0 ; c < ALPACA_RETRY_COUNT ; c++ ) {
            try {
                if ( c != 0 ) {
                    logger.warn( "sleeping then, retrying connection to alpaca." );
                    Thread.sleep( ALPACA_RETRY_DELAY_MS );
                }

                JsonClient.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, method, jsonObject );

                return null;
            } catch ( JsonClient.ConnectionException e ) {
                Throwable cause = e.getCause();
                
                if ( cause == null ) {
                    return e;
                }
                
                if ( cause instanceof ConnectTimeoutException ) {
                    logger.warn( "timeout communicating with the alpaca, trying again." );
                    continue;
                }
                
                if ( cause instanceof ConnectException ) {
                    logger.warn( "unable to connect to alpaca, sleeping and trying again.");
                    try {
                        Thread.sleep(1000);
                    } catch ( InterruptedException ie ) {
                        logger.warn( "Interrupted while retrying, continuing");
                    }
                    continue;
                }
                
                return e;
            } catch(Exception e) {
                return e;
            }            
        }

        return null;
    }

    /* Create a networking manager, this is a first come first serve
     * basis.  The first class to create the network manager gets a
     * networking manager, all other classes get AccessException.  Done
     * this way so only the UvmContextImpl can create a networking manager
     * and then give out access to those classes (argon) that need it.
     * RBS (2/19/06) this is kind of silly, and annoying, switching to getInstance.
     */
    public synchronized static NetworkManagerImpl getInstance()
    {
        if ( INSTANCE != null ) return INSTANCE;

        INSTANCE = new NetworkManagerImpl();
        return INSTANCE;
    }

    /* Load the network configuration */
    private NetworkConfiguration loadNetworkConfiguration()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        NetworkConfiguration settings = null;
        
        try {
            settings = settingsManager.load(NetworkConfiguration.class, "/etc/untangle-net-alpaca/netConfig");
        } catch (SettingsManager.SettingsException e) {
            logger.error("Unable to read netConfig file: ", e );
            return null;
        }

        if (settings == null) {
            logger.error("Failed to read network settings");
        } 
        
        return settings;
    }
}
