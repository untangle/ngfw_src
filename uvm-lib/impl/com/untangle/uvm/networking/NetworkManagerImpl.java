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

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.untangle.jnetcap.Netcap;
import com.untangle.uvm.ArgonException;
import com.untangle.uvm.ArgonManager;
import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.localapi.SessionMatcherFactory;
import com.untangle.uvm.networking.internal.AccessSettingsInternal;
import com.untangle.uvm.networking.internal.AddressSettingsInternal;
import com.untangle.uvm.networking.internal.MiscSettingsInternal;
import com.untangle.uvm.networking.internal.NetworkSpaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.uvm.networking.internal.ServicesInternalSettings;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPSessionDesc;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.LocalNodeManager;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.ValidateException;
import com.untangle.uvm.node.script.ScriptRunner;
import com.untangle.uvm.node.script.ScriptWriter;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.toolbox.RemoteToolboxManager;
import com.untangle.uvm.util.XMLRPCUtil;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import static com.untangle.uvm.networking.ShellFlags.FILE_RULE_CFG;

/* XXX This shouldn't be public */
public class NetworkManagerImpl implements LocalNetworkManager
{
    private static NetworkManagerImpl INSTANCE = null;

    static final String ETC_INTERFACES_FILE = "/etc/network/interfaces";
    static final String ETC_RESOLV_FILE = "/etc/resolv.conf";

    private final Logger logger = Logger.getLogger(getClass());

    static final String NAT_NODE_NAME = "untangle-node-router";

    static final String BUNNICULA_BASE = System.getProperty( "bunnicula.home" );
    static final String BUNNICULA_CONF = System.getProperty( "bunnicula.conf.dir" );

    static final String ALPACA_SCRIPT = "/usr/share/untangle-net-alpaca/scripts/";

    /* Script to run after reconfiguration (from NetworkSettings Listener) */
    private static final String AFTER_RECONFIGURE_SCRIPT = BUNNICULA_BASE + "/networking/after-reconfigure";

    private static final String SINGLE_NIC_FLAG = "8e1f48a294372f872b74fedec79696a8";

    /* A flag for devel environments, used to determine whether or not
     * the etc files actually are written, this enables/disables reconfiguring networking */
    private boolean saveSettings = true;

    /* Inidicates whether or not the networking manager has been initialized */
    private boolean isInitialized = false;

    /* Indicates whether or not single NIC mode is enabled */
    private boolean isSingleNicModeEnabled = false;

    /* Manager for the iptables rules */
    private final RuleManager ruleManager;

    /* Manager for AccessSettings */
    private final AccessManagerImpl accessManager;

    /* Manager for AddressSettings */
    private final AddressManagerImpl addressManager;

    /* Manager for MiscSettings */
    private final MiscManagerImpl miscManager;

    /* ??? Does the order matter, it shouldn't.  */
    private Set<NetworkSettingsListener> networkListeners = new HashSet<NetworkSettingsListener>();
    private Set<IntfEnumListener> intfEnumListeners = new HashSet<IntfEnumListener>();

    /** The nuts and bolts of networking, the real bits of panther.  this my friend
     * should never be null */
    private NetworkSpacesInternalSettings networkSettings = null;
    
    /** The current services settings */
    private ServicesInternalSettings servicesSettings = null;

    /* the netcap  */
    private final Netcap netcap = Netcap.getInstance();

    /* the address of the internal interface, used for the web address */
    private InetAddress internalAddress;

    /* True if Dynamic DNS is available */
    private boolean isDynamicDnsEnabled = false;

    /* Flag to indicate when the UVM has been shutdown */
    private boolean isShutdown = false;

    /* Bogus IP Address used to guarantee an address exists. */
    private final IPaddr bogusAddress;

    private NetworkManagerImpl()
    {
        this.ruleManager = RuleManager.getInstance();
        this.accessManager = new AccessManagerImpl();
        this.addressManager = new AddressManagerImpl();
        this.miscManager = new MiscManagerImpl();

        IPaddr address = null;
        try {
            address = IPaddr.parse( "192.0.2.1" );
        } catch ( ParseException e ) {
            /* This should never happen */
            address = null;
        } catch ( UnknownHostException e ) {
            /* This should never happen */
            address = null;
        }
        this.bogusAddress = address;
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
        BasicNetworkSettings basic = NetworkUtilPriv.getPrivInstance().toBasic( this.networkSettings );
        return basic;
    }

    /**
     * Retrieve the settings related to limiting access to the box.
     */
    public AccessSettings getAccessSettings()
    {
        return this.accessManager.getSettings();
    }

    public AccessSettingsInternal getAccessSettingsInternal()
    {
        return this.accessManager.getInternalSettings();
    }

    public void setAccessSettings( AccessSettings access )
    {
        this.accessManager.setSettings( access );
    }

    /**
     * Retrieve the settings related to the hostname and the address used to access to the box.
     */
    public AddressSettings getAddressSettings()
    {
        return this.addressManager.getSettings();
    }

    public AddressSettingsInternal getAddressSettingsInternal()
    {
        return this.addressManager.getInternalSettings();
    }

    public void setAddressSettings( AddressSettings address )
    {
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

        /* Send the call onto the alpaca */
        PPPoEConnectionRule pppoe = basic.getPPPoESettings();

        Object args[] = null;
        String method = null;

        if ( pppoe.isLive()) {
            /* PPPoE Setup */
            args = new String[2];
            method = "wizard_external_interface_pppoe";
            args[0] = pppoe.getUsername();
            args[1] = pppoe.getPassword();
        } else if ( basic.isDhcpEnabled()) {
            /* Dynamic address */
            args = new String[0];
            method = "wizard_external_interface_dynamic";
        } else {
            /* Must be a static address */
            args = new String[5];
            args[0] = basic.host().toString();
            args[1] = basic.netmask().toString();
            args[2] = basic.gateway().toString();
            args[3] = basic.dns1().toString();
            args[4] = "";
            
            IPaddr dns2 = basic.dns2();
            if ( !dns2.isEmpty()) args[4] = dns2.toString();
            method = "wizard_external_interface_static";
        }

        /* Make a synchronous request */
        try {
            XMLRPCUtil.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, method, null, args );
        } catch ( Exception e ) {
            throw new NetworkException( "Unable to configure the external interface.", e );
        }
    }

    public void setWizardNatEnabled( IPaddr address, IPaddr netmask )
    {
        try{
            boolean hasChanged = true;

            if ( NetworkUtil.DEFAULT_NAT_ADDRESS.equals( address ) &&
                 NetworkUtil.DEFAULT_NAT_NETMASK.equals( netmask )) {
                hasChanged = false;
            }

            logger.debug( "enabling nat as requested by setup wizard: " + address + "/" + netmask );

            /* Make a synchronous request */
            try {
                XMLRPCUtil.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, 
                                                     "wizard_internal_interface_nat", null,
                                                     address.toString(), netmask.toString());
            } catch ( Exception e ) {
                logger.warn( "Unable to enable NAT.", e );
            }

            if ( !hasChanged ) LocalUvmContextFactory.context().adminManager().logout();
        }
        catch(Exception e){
            logger.error( "Error setting up NAT in wizard", e );
        }
    }

    public void setWizardNatDisabled()
    {
        LocalUvmContextFactory.context().adminManager().logout();

        logger.debug( "disabling nat as requested by setup wizard: " );

        /* Make a synchronous request */
        try {
            XMLRPCUtil.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, 
                                                 "wizard_internal_interface_bridge", null );
        } catch ( Exception e ) {
            logger.warn( "Unable to disable NAT for wizard", e );
        }

        // We no longer uninstall the router if nat is disabled.
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
        return this.isSingleNicModeEnabled;
    }

    public void updateAddress()
    {
        /* Get the new address for any dhcp spaces */
        NetworkSpacesInternalSettings previous = this.networkSettings;

        synchronized( this ) {
            try {
                LocalUvmContextFactory.context().localIntfManager().loadInterfaceConfig();
            } catch ( Exception e ) {
                logger.error( "Exception loading the interface configuration.", e );
            }

            try {
                /* Update the address database in netcap */
                Netcap.getInstance().updateAddress();
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

        /* Update the internal address */
        if ( SINGLE_NIC_FLAG.equals( properties.getProperty( "com.untangle.networking.single-nic-mode" ))) {
            setSingleNicMode( true );
        } else {
            setSingleNicMode( false );
        }
        
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

        /* ignore everything on the external or dmz interface */
        if ( argonIntf == IntfConstants.EXTERNAL_INTF || argonIntf == IntfConstants.DMZ_INTF ) return null;

        if ( this.isSingleNicModeEnabled ) argonIntf = IntfConstants.EXTERNAL_INTF;
        
        /* Retrieve the network settings */
        NetworkSpacesInternalSettings settings = this.networkSettings;

        if ( settings == null ) return null;

        NetworkSpaceInternal local = settings.getNetworkSpace( argonIntf );
        
        if ( local == null ) return null;

        IPNetwork network = local.getPrimaryAddress();

        if ( network == null ) return null;

        IPaddr address = network.getNetwork();

        if ( address == null ) return null;

        if ( address.equals( this.bogusAddress )) return null;
        
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

    /* Intf enum settings */
    private void callIntfEnumListeners()
    {
        logger.error( "interface enum listeners are unsupported.", new Exception());
        if ( true || !true ) return;

        for ( IntfEnumListener listener : this.intfEnumListeners ) {
            try {
                listener.event( null );
            } catch ( Exception e ) {
                logger.error( "Exception calling listener", e );
            }
        }
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
        String disableSaveSettings = System.getProperty( "bunnicula.devel.nonetworking" );

        /* Do not save settings if requested */
        if ( Boolean.valueOf( disableSaveSettings ) == true ) {
            this.saveSettings = false;
        }

        loadAllSettings();

        NetworkUtilPriv nup = NetworkUtilPriv.getPrivInstance();

        /* Done before so these get called on the first update */
        registerListener(new IPMatcherListener());
        registerListener(new CifsListener());

        updateAddress();

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

    private void setSingleNicMode( boolean newValue )
    {
        logger.debug( "setSingleNicMode(" + newValue + ")" );

        if ( newValue != this.isSingleNicModeEnabled ) {
            logger.info( "Changing the state of single NIC mode[" + newValue + "], killall all sessions." );
            
            ArgonManager argonManager = LocalUvmContextFactory.context().argonManager();
            argonManager.shutdownMatches(SessionMatcherFactory.getAllInstance());
        }
        
        this.isSingleNicModeEnabled = newValue;
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
