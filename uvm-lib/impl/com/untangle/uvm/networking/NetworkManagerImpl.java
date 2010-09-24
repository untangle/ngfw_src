/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
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

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.jnetcap.Netcap;
import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.localapi.ArgonInterface;
import com.untangle.uvm.networking.internal.AccessSettingsInternal;
import com.untangle.uvm.networking.internal.AddressSettingsInternal;
import com.untangle.uvm.networking.internal.InterfaceInternal;
import com.untangle.uvm.networking.internal.MiscSettingsInternal;
import com.untangle.uvm.networking.internal.NetworkSpaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.uvm.networking.internal.ServicesInternalSettings;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPSessionDesc;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.ValidateException;
import com.untangle.uvm.node.firewall.ip.IPMatcher;
import com.untangle.uvm.node.firewall.ip.IPMatcherFactory;
import com.untangle.uvm.node.script.ScriptRunner;
import com.untangle.uvm.node.script.ScriptWriter;
import com.untangle.uvm.util.JsonClient;
import com.untangle.uvm.util.XMLRPCUtil;

/* XXX This shouldn't be public */
public class NetworkManagerImpl implements LocalNetworkManager
{
    private static NetworkManagerImpl INSTANCE = null;

    static final String ETC_INTERFACES_FILE = "/etc/network/interfaces";
    static final String ETC_RESOLV_FILE = "/etc/resolv.conf";

    private final Logger logger = Logger.getLogger(getClass());

    static final String NAT_NODE_NAME = "untangle-node-router";

    static final String UVM_BASE = System.getProperty( "uvm.home" );
    static final String UVM_CONF = System.getProperty( "uvm.conf.dir" );

    static final String ALPACA_SCRIPT = "/usr/share/untangle-net-alpaca/scripts/";

    /* Script to run after reconfiguration (from NetworkSettings Listener) */
    private static final String AFTER_RECONFIGURE_SCRIPT = UVM_BASE + "/networking/after-reconfigure";

    /* Script to run to get a list of physical interfaces */
    private static final String GET_PHYSICAL_INTF_SCRIPT = UVM_BASE + "/networking/get-physical-interfaces";


    private static final long ALPACA_RETRY_COUNT = 3;
    private static final long ALPACA_RETRY_DELAY_MS = 6000;

    /* A flag for devel environments, used to determine whether or not
     * the etc files actually are written, this enables/disables reconfiguring networking */
    private boolean saveSettings = true;

    /* Inidicates whether or not the networking manager has been initialized */
    private boolean isInitialized = false;

    /* Manager for the iptables rules */
    private final RuleManager ruleManager;

    /* Manager for AccessSettings */
    private final AccessManagerImpl accessManager;

    /* Manager for AddressSettings */
    private final AddressManagerImpl addressManager;

    /* Manager for MiscSettings */
    private final MiscManagerImpl miscManager;

    /* Manager for single nic mode. */
    private final SingleNicManager singleNicManager;

    /* ??? Does the order matter, it shouldn't.  */
    private Set<NetworkSettingsListener> networkListeners = new HashSet<NetworkSettingsListener>();
    private Set<IntfEnumListener> intfEnumListeners = new HashSet<IntfEnumListener>();

    /** The nuts and bolts of networking, the real bits of panther.  this my friend
     * should never be null */
    private NetworkSpacesInternalSettings networkSettings = null;

    /** The current services settings */
    private ServicesInternalSettings servicesSettings = null;

    /* the address of the internal interface, used for the web address */
    private InetAddress internalAddress;

    /* True if Dynamic DNS is available */
    private boolean isDynamicDnsEnabled = false;

    /* Flag to indicate when the UVM has been shutdown */
    private boolean isShutdown = false;

    private HostName domainName = null;

    private NetworkManagerImpl()
    {
        this.ruleManager = RuleManager.getInstance();
        this.accessManager = new AccessManagerImpl();
        this.addressManager = new AddressManagerImpl();
        this.miscManager = new MiscManagerImpl();
        this.singleNicManager = new SingleNicManager();
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
        InterfaceTester.getInstance().updateLinkStatus( this.networkSettings );
    }

    /* Return the primary address of the device, this is the primary
     * external address.  which is the first address registered on the
     * first network space */
    IPaddr getPrimaryAddress()
    {
        NetworkSpacesInternalSettings settings = this.networkSettings;

        if ( settings == null ) return null;

        NetworkSpaceInternal external = settings.getNetworkSpace( IntfConstants.EXTERNAL_INTF );

        if ( external == null ) return null;

        return external.getPrimaryAddress().getNetwork();
    }

    public BasicNetworkSettings getBasicSettings()
    {
        boolean snic = this.singleNicManager.getIsEnabled();
        BasicNetworkSettings basic = NetworkUtilPriv.getPrivInstance().toBasic( this.networkSettings, snic );
        return basic;
    }

    /**
     * Retrieve the settings related to limiting access to the box.
     */
    @Override
    public AccessSettings getAccessSettings()
    {
        return this.accessManager.getSettings();
    }

    public AccessSettingsInternal getAccessSettingsInternal()
    {
        return this.accessManager.getInternalSettings();
    }

    @Override
    public void setAccessSettings( AccessSettings access )
    {
        this.accessManager.setSettings( access );

        updateAddress();

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
        logger.warn("getAddressSettings()");
        return this.addressManager.getSettings();
    }

    public AddressSettingsInternal getAddressSettingsInternal()
    {
        return this.addressManager.getInternalSettings();
    }

    @Override
    public void setAddressSettings( AddressSettings address )
    {
        logger.warn("setAddressSettings(" + address.getHttpsPort() + ")" );
        this.addressManager.setSettings( address );

        updateAddress();

        try {
            generateRules();
        } catch ( Exception e ) {
            logger.warn( "Unable to generate rules.", e );
        }
    }

    /**
     * Retrieve the miscellaneous settings that don't really belong anywhere else.
     */
    public MiscSettings getMiscSettings()
    {
        return this.miscManager.getSettings();
    }

    public MiscSettingsInternal getMiscSettingsInternal()
    {
        return this.miscManager.getInternalSettings();
    }

    public void setMiscSettings( MiscSettings misc )
    {
        this.miscManager.setSettings( misc );

        updateAddress();

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
        } catch ( NetworkException e ) {
            logger.error( "Unable to create rules", e );
        }
    }

    /* Remove a service that needs outside access to HTTPs, the name should be unique */
    public synchronized void unregisterService( String name )
    {
        this.accessManager.unregisterService( name );

        try {
            generateRules();
        } catch ( NetworkException e ) {
            logger.error( "Unable to create rules", e );
        }
    }

    public NetworkSpacesSettingsImpl getNetworkSettings()
    {
        return NetworkUtilPriv.getPrivInstance().toSettings( this.networkSettings );
    }

    public NetworkSpacesInternalSettings getNetworkInternalSettings()
    {
        return this.networkSettings;
    }

    public List<Interface> getInterfaceList( boolean updateStatus )
    {
        if ( updateStatus ) updateLinkStatus();

        List<InterfaceInternal> internalList = this.networkSettings.getInterfaceList();
        List<Interface> interfaceList = new ArrayList<Interface>( internalList.size());
        for ( InterfaceInternal internal : internalList ) interfaceList.add( internal.toInterface());
        return interfaceList;
    }

    public void remapInterfaces( String[] osArray, String[] userArray ) throws NetworkException
    {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put( "os_names", new JSONArray( osArray ));
            jsonObject.put( "user_names", new JSONArray( userArray ));
        } catch ( JSONException e ) {
            throw new NetworkException( "Unable to build JSON Object", e );
        }

        try {
            JsonClient.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, "remap_interfaces", jsonObject );
        } catch ( Exception e ) {
            throw new NetworkException( "Unable to configure the external interface.", e );
        }
    }

    /* Set the access and address settings, used by the Remote Panel */
    public void setSettings( AccessSettings access, AddressSettings address )
        throws NetworkException, ValidateException
    {
        this.accessManager.setSettings( access );
        this.addressManager.setSettings( address );

        updateAddress();

        generateRules();
    }

    /* Set the Access, Misc and Network settings at once.  Used by the
     * support panel */
    public void setSettings( AccessSettings access, MiscSettings misc )
        throws NetworkException, ValidateException
    {
        this.accessManager.setSettings( access );
        this.miscManager.setSettings( misc );

        updateAddress();

        generateRules();
    }

    public ServicesInternalSettings getServicesInternalSettings()
    {
        return this.servicesSettings;
    }

    /* Returns true if dynamic dns is enabled */
    boolean isDynamicDnsEnabled()
    {
        return this.isDynamicDnsEnabled;
    }

    /* Get the current hostname */
    public HostName getHostname()
    {
        return this.addressManager.getInternalSettings().getHostName();
    }

    /* Get the domain name suffix */
    public HostName getDomainName()
    {
        if ( this.domainName == null ) {
            return NetworkUtil.LOCAL_DOMAIN_DEFAULT;
        }

        return this.domainName;
    }

    public String getPublicAddress()
    {
        return this.addressManager.getInternalSettings().getCurrentURL();
    }

    /* Get the external HTTPS port */
    public int getPublicHttpsPort()
    {
        return this.addressManager.getInternalSettings().getCurrentPublicPort();
    }

    /* Save the basic network settings during the wizard */
    public void setSetupSettings( AddressSettings address, BasicNetworkSettings basic )
        throws NetworkException, ValidateException
    {
        this.addressManager.setWizardSettings( address );

        setSetupSettings( basic );
    }

    public BasicNetworkSettings setSetupSettings( BasicNetworkSettings basic )
        throws NetworkException, ValidateException
    {
        /* Send the call onto the alpaca */

        JSONObject jsonObject = new JSONObject();
        String method = null;
        boolean isSingleNicEnabled = false;
        PPPoESettings pppoe = basic.getPPPoESettings();

        try {
            if ( pppoe.isLive()) {
                /* PPPoE Setup */
                method = "wizard_external_interface_pppoe";
                jsonObject.put( "username", pppoe.getUsername());
                jsonObject.put( "password", pppoe.getPassword());
            } else if ( basic.getDhcpEnabled()) {
                /* Dynamic address */
                method = "wizard_external_interface_dynamic";
                isSingleNicEnabled = basic.isSingleNicEnabled();
            } else {
                /* Must be a static address */
                jsonObject.put( "ip", basic.getHost().toString());
                jsonObject.put( "netmask", basic.getNetmask().toString());
                jsonObject.put( "default_gateway", basic.getGateway().toString());
                jsonObject.put( "dns_1", basic.getDns1().toString());
                
                IPaddr dns2 = basic.getDns2();
                if ( !dns2.isEmpty()) jsonObject.put( "dns_2", dns2 );
                method = "wizard_external_interface_static";
                isSingleNicEnabled = basic.isSingleNicEnabled();
            }

            jsonObject.put( "single_nic_mode", isSingleNicEnabled );
        } catch ( JSONException e ) {
            throw new NetworkException( "Unable to build JSON Object", e );
        }

        /* Make a synchronous request */
        Exception e = retryAlpacaCall( method, jsonObject );
        if ( e != null ) {
            logger.warn( "Unable to configure the external interface.", e );
            throw new NetworkException( "Unable to configure the external interface.", e );
        }

        return getBasicSettings();
    }

    /* returns a recommendation for the internal network. */
    /* @param externalAddress The external address, if null, this uses
     * the external address of the box. */
    public IPNetwork getWizardInternalAddressSuggesstion(IPaddr externalAddress)
    {
        try {
            if ( externalAddress == null ) { 
                boolean snic = this.singleNicManager.getIsEnabled();

                BasicNetworkSettings basic = NetworkUtilPriv.getPrivInstance().toBasic( this.networkSettings, snic );
                externalAddress = basic.getHost();
            }

            /* rare case. */
            if ( externalAddress == null ) {
                return IPNetwork.parse( "172.16.0.1/24" );
            }

            IPMatcher matcher = IPMatcherFactory.parse( "192.0.0.0/8" );
            
            if ( matcher.isMatch( externalAddress.getAddr())) {
                return IPNetwork.parse( "172.16.0.1/24" );
            }
            
            return IPNetwork.parse( "192.168.1.1/24" );
        } catch ( Exception e ) {
            /* This should never happen */
            throw new RuntimeException( "Unable to suggest an internal address", e );
        }
    }

    public void setWizardNatEnabled( IPaddr address, IPaddr netmask, boolean enableDhcpServer )
        throws NetworkException
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
            throw new NetworkException( "Unable to build JSON Object", e );
        }
        Exception e = retryAlpacaCall( "wizard_internal_interface_nat", jsonObject );

        if ( e != null ) {
            logger.warn( "unable to setup system for NAT in wizard.", e );
            throw new NetworkException( "Unable to enable nat settings.", e );
        }
    }

    public void setWizardNatDisabled() throws NetworkException
    {
        logger.debug( "disabling nat in setup wizard: " );

        /* Make a synchronous request */
        Exception e = retryAlpacaCall( "wizard_internal_interface_bridge", null );
        if ( e != null ) {
            logger.warn( "Unable to disable NAT in wizard", e );
            throw new NetworkException( "Unable to disable NAT.", e );
        }
    }

    /* Returns true if address is local to the edgeguard */
    public boolean isAddressLocal( IPaddr address )
    {
        NetworkUtilPriv nup = NetworkUtilPriv.getPrivInstance();

        return nup.isAddressLocal( this.networkSettings, address );
    }

    /* Returns true if single nic mode is enabled */
    public boolean isSingleNicModeEnabled()
    {
        return this.singleNicManager.getIsEnabled();
    }
    
    /* Return a list of the physical interfaces on the box */
    public List<String> getPhysicalInterfaceNames() throws NetworkException
    {
        List<String> names = new LinkedList<String>();

        try {
            String values = ScriptRunner.getInstance().exec( GET_PHYSICAL_INTF_SCRIPT );
            for ( String value : values.split( "[\\s\\n]" )) {
                names.add( value );
            }
        } catch ( Exception e ) {
            logger.error( "Unable to get list of interfaces.", e );
            throw new NetworkException( "Unable to get list of interfaces." );
        }

        return names;
    }

    public void singleNicRegisterAddress( InetAddress address )
    {
        this.singleNicManager.registerAddress( address );
    }

    public Boolean isQosEnabled()
    {
        try {
            JSONObject jsonObject = JsonClient.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, "get_qos_settings", null );

            JSONObject result = jsonObject.getJSONObject("result");
            if (result == null)
                return Boolean.FALSE;

            Boolean enabled = result.getBoolean("enabled");
            if (enabled == null) {
                logger.warn("Unable to retrieve QoS settings: null");
            }
            return enabled;
            
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
    
    public void updateAddress()
    {
        synchronized( this ) {
            try {
                LocalUvmContextFactory.context().localIntfManager().loadInterfaceConfig();
            } catch ( Exception e ) {
                logger.error( "Exception loading the interface configuration.", e );
            }

            try {
                /* Update the address database in netcap */
                Netcap.updateAddress();
            } catch ( Exception e ) {
                logger.error( "Exception updating address.", e );
            }
        }

        NetworkUtilPriv nup = NetworkUtilPriv.getPrivInstance();

        Properties properties = null;

        try {
            properties = nup.loadProperties();
        } catch ( Exception e ) {
            logger.warn( "Unable to load the network properties, using empty properties.", e );
            properties = new Properties();
        }

        this.networkSettings = NetworkUtilPriv.getPrivInstance().loadNetworkSettings( properties );

        /* Load whether or not dynamic DNS is enabled */
        this.isDynamicDnsEnabled =
            Boolean.parseBoolean( properties.getProperty( "com.untangle.networking.ddns-en" ));
        logger.debug( "DynamicDns: " + this.isDynamicDnsEnabled );

        
        String domainName = properties.getProperty( "com.untangle.networking.domain-suffix" );
        try {

            if (( domainName != null ) && ( domainName.trim().length() > 0 )) {
                this.domainName = HostName.parseStrict( domainName.trim());
            }
        } catch ( Exception e ) {
            /* */
            logger.warn( "Unable to parse the domain name: <" + domainName + ">" );
        }

        /* Load the internal address (has to happen before the services settings are loaded) */
        updateInternalAddress( this.networkSettings );
        logger.debug( "New internal address is: '" + this.internalAddress + "'" );
        IPaddr serviceAddress = NetworkUtil.BOGUS_DHCP_ADDRESS;
        if ( this.internalAddress != null ) serviceAddress = new IPaddr( this.internalAddress );

        this.servicesSettings =
            NetworkUtilPriv.getPrivInstance().loadServicesSettings( properties, serviceAddress );

        this.addressManager.updateAddress( properties );

        if ( logger.isDebugEnabled()) {
            logger.debug( "New network settings: " + this.networkSettings );
            logger.debug( "New services settings: " + this.servicesSettings );
        }

        /* XXX This should be rethought,but it is important for the firewall
         * rules for the QA push XXXX */
        try {
            ScriptWriter scriptWriter = new ScriptWriter();
            /* Set whether or not setup has completed */

            this.accessManager.commit( scriptWriter );
            this.addressManager.commit( scriptWriter );
            this.miscManager.commit( scriptWriter );
            this.ruleManager.commit( scriptWriter );

            /* Save out the script */
            scriptWriter.writeFile( FILE_RULE_CFG );
        } catch ( Exception e ) {
            logger.warn( "Error committing the networking.sh file", e );
        }

        /* Update single nic mode. */
        this.singleNicManager.setIsEnabled( properties.getProperty( "com.untangle.networking.single-nic-mode" ));
        try {
            callNetworkListeners();
        } catch ( Exception e ) {
            logger.error( "Exception in a listener", e );
        }
    }

    /*
     * This returns an address where the host should be able to access
     * HTTP.  if HTTP is not reachable, this returns NULL
     */
    public InetAddress getInternalHttpAddress( IPSessionDesc session )
    {
        byte argonIntf = session.clientIntf();
        ArgonInterface ai = null;

        try {
            ai = LocalUvmContextFactory.context().localIntfManager().getIntfByArgon( argonIntf );
        } catch ( Exception e ) {
            ai = null;
        }

        if ( ai == null ) return null;
        if ( ai.isWanInterface()) return null;

        /* In single nicmode, use the external interface. */
        if ( this.singleNicManager.getIsEnabled()) argonIntf = IntfConstants.EXTERNAL_INTF;
        
        /* Retrieve the network settings */
        NetworkSpacesInternalSettings settings = this.networkSettings;

        if ( settings == null ) return null;

        NetworkSpaceInternal local = settings.getNetworkSpace( argonIntf );

        if ( local == null ) return null;

        IPNetwork network = local.getPrimaryAddress();

        if ( network == null ) return null;

        IPaddr address = network.getNetwork();

        if ( address == null ) return null;

        if ( NetworkUtilPriv.getPrivInstance().isBogus( address )) return null;

        return address.getAddr();
    }

    /* Update all of the iptables rules and the inside address database */
    private void generateRules() throws NetworkException
    {
        if ( this.isShutdown ) return;

        ScriptWriter scriptWriter = new ScriptWriter();
        this.accessManager.commit( scriptWriter );
        this.addressManager.commit( scriptWriter );
        this.miscManager.commit( scriptWriter );
        this.ruleManager.commit( scriptWriter );

        /* Save out the script */
        scriptWriter.writeFile( FILE_RULE_CFG );

        this.ruleManager.generateIptablesRules();
    }

    public void isShutdown()
    {
        this.isShutdown = true;
        this.ruleManager.isShutdown();
        this.singleNicManager.stop();
    }

    public void flushIPTables() throws NetworkException
    {
        this.ruleManager.destroyIptablesRules();
    }

    /* Listener functions */
    private void callNetworkListeners()
    {
        logger.debug( "Calling network listeners." );
        for ( NetworkSettingsListener listener : this.networkListeners ) {
            if ( logger.isDebugEnabled()) logger.debug( "Calling listener: " + listener );

            try {
                listener.event( this.networkSettings );
            } catch ( Exception e ) {
                logger.error( "Exception calling listener", e );
            }
        }
        logger.debug( "Done calling network listeners." );
    }

    public void registerListener( NetworkSettingsListener networkListener )
    {
        this.networkListeners.add( networkListener );
    }

    public void unregisterListener( NetworkSettingsListener networkListener )
    {
        this.networkListeners.remove( networkListener );
    }

    /* Address settings */
    public void registerListener( AddressSettingsListener addressListener )
    {
        this.addressManager.registerListener( addressListener );
    }

    public void unregisterListener( AddressSettingsListener addressListener )
    {
        this.addressManager.unregisterListener( addressListener );
    }

    // Interface enums listener are presently unsupported
    public void registerListener( IntfEnumListener intfEnumListener )
    {
        logger.error( "interface enum listeners are unsupported.", new Exception());
        this.intfEnumListeners.add( intfEnumListener );
    }

    public void unregisterListener( IntfEnumListener intfEnumListener )
    {
        logger.error( "interface enum listeners are unsupported.", new Exception());
        this.intfEnumListeners.remove( intfEnumListener );
    }

    /* ----------------- Package ----------------- */
    boolean getSaveSettings()
    {
        return this.saveSettings;
    }

    /* ----------------- Private ----------------- */
    private void initPriv() throws NetworkException, ValidateException
    {
        String disableSaveSettings = System.getProperty( "uvm.devel.nonetworking" );

        /* Do not save settings if requested */
        if ( Boolean.valueOf( disableSaveSettings ) == true ) {
            this.saveSettings = false;
        }

        loadAllSettings();

        /* Done before so these get called on the first update */
        registerListener(new IPMatcherListener());
        registerListener(new IntfMatcherListener());
        registerListener(new CifsListener());
        registerListener(this.singleNicManager.getListener());

        updateAddress();

        this.singleNicManager.start();

        try {
            generateRules();
        } catch ( Exception e ) {
            logger.error( "Exception generating rules", e );
        }

        /* Update the link status for all of the interfaces */
        updateLinkStatus();

        // Register the built-in listeners
        registerListener(new AfterReconfigureScriptListener());
    }

    /* Methods for saving and loading the settings files from the database at startup */
    private void loadAllSettings()
    {
        /* Load the miscellaneous settings */
        this.miscManager.init();

        /* Load the access settings. */
        this.accessManager.init();

        /* Load the address/hostname settings */
        this.addressManager.init();
    }

    /* Method to calculate what the current internal address is */
    private void updateInternalAddress( NetworkSpacesInternalSettings settings )
    {
        this.internalAddress = null;
        if ( settings == null ) return;

        NetworkSpaceInternal internal = settings.getNetworkSpace( IntfConstants.INTERNAL_INTF );

        if ( internal == null ) return;

        IPaddr address = internal.getPrimaryAddress().getNetwork();
        this.internalAddress = address.getAddr();
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

    class AfterReconfigureScriptListener implements NetworkSettingsListener
    {
        public void event( NetworkSpacesInternalSettings settings )
        {
            /* Run the script */
            try {
                ScriptRunner.getInstance().exec( AFTER_RECONFIGURE_SCRIPT );
            } catch ( Exception e ) {
                logger.error( "Error running after reconfigure script", e );
            }
        }
    }
}
