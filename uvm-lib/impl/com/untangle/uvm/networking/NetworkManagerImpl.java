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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.untangle.jnetcap.Netcap;
import com.untangle.uvm.ArgonException;
import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
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
import com.untangle.uvm.node.ValidateException;
import com.untangle.uvm.node.script.ScriptRunner;
import com.untangle.uvm.node.script.ScriptWriter;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.toolbox.RemoteToolboxManager;
import com.untangle.uvm.util.DataLoader;
import com.untangle.uvm.util.DataSaver;
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

    /* Script to run after reconfiguration (from NetworkSettings Listener) */
    private static final String AFTER_RECONFIGURE_SCRIPT = BUNNICULA_BASE + "/networking/after-reconfigure";

    /* Write the expected bridge configuration here, if it is in place, then
     * bridges are not regenerated each time the network is saved */
    private static final String BRIDGE_CFG_FILE = BUNNICULA_CONF + "/bridge_cfg";

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

    /* Converter to create the initial networking configuration object if
     * network spaces has never been executed before */
    private NetworkConfigurationLoader networkConfigurationLoader;

    /* ??? Does the order matter, it shouldn't.  */
    private Set<NetworkSettingsListener> networkListeners = new HashSet<NetworkSettingsListener>();
    private Set<IntfEnumListener> intfEnumListeners = new HashSet<IntfEnumListener>();

    /** The nuts and bolts of networking, the real bits of panther.  this my friend
     * should never be null */
    private NetworkSpacesInternalSettings networkSettings = null;

    /* the netcap  */
    private final Netcap netcap = Netcap.getInstance();

    /* the address of the internal interface, used for the web address */
    private InetAddress internalAddress;

    /* Flag to indicate when the UVM has been shutdown */
    private boolean isShutdown = false;

    private NetworkManagerImpl()
    {
        this.ruleManager = RuleManager.getInstance();
        this.networkConfigurationLoader = NetworkConfigurationLoader.getInstance();
        this.accessManager = new AccessManagerImpl();
        this.addressManager = new AddressManagerImpl();
        this.miscManager = new MiscManagerImpl();
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

        List<NetworkSpaceInternal> spaceList = settings.getNetworkSpaceList();
        if ( spaceList.size() < 1 ) return null;

        return spaceList.get( 0 ).getPrimaryAddress().getNetwork();
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

    public void setNetworkSettings( NetworkSpacesSettings settings )
        throws NetworkException, ValidateException
    {
        setNetworkSettings( settings, true );
    }

    /**
     * @params:
     * @param: settings - Network settings to save.
     * @param: configure - Set to true in order to configure the new settings, this is the default
     *                   - and is really only used as false by the router..
     */
    public synchronized void setNetworkSettings( NetworkSpacesSettings settings, boolean configure )
        throws NetworkException, ValidateException
    {
        if ( logger.isDebugEnabled()) logger.debug( "Loading the new network settings: " + settings );

        NetworkUtilPriv nup = NetworkUtilPriv.getPrivInstance();
        NetworkSpacesInternalSettings internal = nup.toInternal( settings );;

        /* If the saving is disable, then don't actually reconfigured,
         * just save the settings to the database. */
        if ( configure ) {
            setNetworkSettings( internal );
        } else {
            logger.info( "Configuring network settings disabled, only writing to database" );

            /* In order to save it has to be an Impl, convert from
             * internal and then back out again to get an impl */
            saveNetworkSettings( nup.toSettings( internal ));
        }
    }

    private synchronized void setNetworkSettings( NetworkSpacesInternalSettings newSettings )
        throws NetworkException, ValidateException
    {
        NetworkUtilPriv nup = NetworkUtilPriv.getPrivInstance();

        if ( logger.isDebugEnabled()) logger.debug( "Loading the new network settings: " + newSettings );

        /* Write the settings */
        writeConfiguration( newSettings );

        /* Actually load the new settings */
        if ( this.saveSettings ) {

            /* temporary fix for bug 2693. secret field doesn't run until second save. */
            try {
                ScriptWriter scriptWriter = new ScriptWriter();
                this.miscManager.commit( scriptWriter );
                scriptWriter.writeFile( FILE_RULE_CFG );
            } catch ( Exception e ) {
                /* XXXXXXX not totally sure what to do here, kind of boned */
                logger.warn( "Error writing misc scripts" );
            }
        } else {
            logger.warn( "Not loading new network settings because networking is disabled" );
        }

        /* Save the configuration to the database */
        try {
            saveNetworkSettings( nup.toSettings( newSettings ));
        } catch ( Exception e ) {
            logger.error( "Unable to save settings, updating address anyway", e );
        }

        /* Have to update the settings */
        updateAddress();
    }

    /* Set the network settings and the address settings at once, used
     * by the networking panel */
    public void setSettings( BasicNetworkSettings basic, AddressSettings address )
        throws NetworkException, ValidateException
    {
        synchronized ( this ) {
            this.addressManager.setSettings( address );
        }

        logger.warn( "implement: setBasicSettings( basic )" );
    }

    /* Set the access and address settings, used by the Remote Panel */
    public void setSettings( AccessSettings access, AddressSettings address )
        throws NetworkException, ValidateException
    {
        synchronized ( this ) {
            this.accessManager.setSettings( access );
            this.addressManager.setSettings( address );
        }

        generateRules();
    }

    /* Set the Access, Misc and Network settings at once.  Used by the
     * support panel */
    public synchronized void setSettings( AccessSettings access, MiscSettings misc,
                                          NetworkSpacesSettings network )
        throws NetworkException, ValidateException
    {
        this.accessManager.setSettings( access );
        this.miscManager.setSettings( misc );
        setNetworkSettings( network );
    }

    public ServicesInternalSettings getServicesInternalSettings()
    {
        logger.error( "getServicesInternalSettings: fixme", new Exception());
        return null;
    }

    /* Returns true if dynamic dns is enabled */
    boolean isDynamicDnsEnabled()
    {
        logger.warn( "isDynamicDnsEnabled: fixme", new Exception());
        
        return false;
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
        this.addressManager.setSettings( address );

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
            if ( !dns2.isEmpty()) args[4] = dns2;
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

            /* Just use the utility function to calculate the new start and end range */
            ServicesSettings servicesSettings = new ServicesSettingsImpl();
            logger.error( "DHCP Start and DHCP End Address are not calculated correctly" );
            IPaddr dhcpStart = servicesSettings.getDhcpStartAddress();
            IPaddr dhcpEnd = servicesSettings.getDhcpEndAddress();

            /* Make a synchronous request */
            try {
                XMLRPCUtil.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, 
                                                     "wizard_internal_interface_nat", null,
                                                     address.toString(), netmask.toString(), 
                                                     dhcpStart.toString(), dhcpEnd.toString());
            } catch ( Exception e ) {
                logger.warn( "Unable to enable NAT.", e );
            }

            if ( !hasChanged ) LocalUvmContextFactory.context().adminManager().logout();

            /* Indicate that the user has completed setup */
            NetworkSpacesSettings newSettings = getNetworkSettings();
            newSettings.setHasCompletedSetup( true );
            setNetworkSettings( newSettings );
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

    public void updateAddress()
    {
        /* Get the new address for any dhcp spaces */
        NetworkSpacesInternalSettings previous = this.networkSettings;

        synchronized( this ) {
            try {
                /* Update the address database in netcap */
                Netcap.getInstance().updateAddress();
                
                this.networkSettings = NetworkUtilPriv.getPrivInstance().updateDhcpAddresses( previous );
                
                this.addressManager.updateAddress();                
            } catch ( Exception e ) {
                logger.error( "Exception updating address, reverting to previous settings", e );
                this.networkSettings = previous;
            }
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

        /* ignore everything on the external or DMZ interface */
        if ( argonIntf == IntfConstants.EXTERNAL_INTF || argonIntf == IntfConstants.DMZ_INTF ) return null;

        return internalAddress;
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

        /* If there are no settings, get the settings from the boxes configuration */
        if ( this.networkSettings == null ) {
            /* Need to create new settings, (The method setNetworkingConfiguration assumes that
             * settings is already set, and cannot be used here) */
            BasicNetworkSettings basic = networkConfigurationLoader.loadBasicNetworkSettings();

            /* Save these settings */
            NetworkSpacesInternalSettings internal = nup.toInternal( basic );

            if ( logger.isDebugEnabled()) {
                logger.debug( "Loaded the configuration:\n" + basic );
                logger.debug( "Converted to:\n" + internal );
            }

            /* Save the network settings */
            setNetworkSettings( internal );
        }

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

    /* Methods for writing the configuration files */
    private void writeConfiguration( NetworkSpacesInternalSettings newSettings ) throws NetworkException
    {
        if ( !this.saveSettings ) {
            /* Set to a warn, because if this gets emailed out, something has gone terribly awry */
            logger.warn( "Not writing configuration files because the debug property was set" );
            return;
        }
    }

    /* Methods for saving and loading the settings files from the database at startup */
    private void loadAllSettings()
    {
        try {
            this.networkSettings = loadNetworkSettings();
        } catch ( Exception e ) {
            logger.error( "Error loading network settings, setting to null to be initialized later", e );
            this.networkSettings = null;
        }

        /* These must load after the dynamic dns and the network settings in order to initialze
         * the public address. */

        /* Load the miscellaneous settings */
        this.miscManager.init();

        /* Load the access settings. */
        this.accessManager.init();

        /* Load the address/hostname settings */
        this.addressManager.init();
    }

    private NetworkSpacesInternalSettings loadNetworkSettings() throws NetworkException, ValidateException
    {
        DataLoader<NetworkSpacesSettingsImpl> loader =
            new DataLoader<NetworkSpacesSettingsImpl>( "NetworkSpacesSettingsImpl",
                                                       LocalUvmContextFactory.context());
        NetworkSpacesSettings dbSettings = loader.loadData();

        /* No database settings */
        if ( dbSettings == null ) {
            logger.info( "There are no network database settings" );
            return null;
        }

        return NetworkUtilPriv.getPrivInstance().toInternal( dbSettings );
    }

    /**
     * RBS: 04/05/2006: After rereading this, it is a little redundant code.
     * the only caller to this function has internal settings and then converts
     * them to settings in order to call this function.  It would make more sense
     * to pass in the internal, and convert those to settings rather than going
     * back and forth twice.
     */
    private void saveNetworkSettings( NetworkSpacesSettingsImpl newSettings )
        throws NetworkException, ValidateException
    {
        DataSaver<NetworkSpacesSettingsImpl> saver =
            new NetworkSettingsDataSaver(LocalUvmContextFactory.context());

        NetworkUtilPriv nup = NetworkUtilPriv.getPrivInstance();

        /* Convert the settings to internal before saving
         * them.  Settings should be legit if they can be converted to internal
         * by converting them first, and then converting them out, this guarantees
         * that valid settings are always saved */
        NetworkSpacesInternalSettings nssi = nup.toInternal( newSettings );
        newSettings = nup.toSettings( nssi );

        NetworkSpacesSettings dbSettings = saver.saveData( newSettings );

        /* No database settings */
        if ( dbSettings == null ) {
            logger.error( "Unable to save the network settings." );
            return;
        }

        this.networkSettings = nssi;
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

class NetworkSettingsDataSaver extends DataSaver<NetworkSpacesSettingsImpl>
{
    public NetworkSettingsDataSaver( LocalUvmContext local )
    {
        super( local );
    }

    protected void preSave( Session s )
    {
        Query q = s.createQuery( "from NetworkSpacesSettingsImpl" );
        for ( Iterator iter = q.iterate() ; iter.hasNext() ; ) {
            NetworkSpacesSettingsImpl settings = (NetworkSpacesSettingsImpl)iter.next();
            s.delete( settings );
        }
    }
}

class ServicesSettingsDataSaver extends DataSaver<ServicesSettingsImpl>
{
    public ServicesSettingsDataSaver( LocalUvmContext local )
    {
        super( local );
    }

    protected void preSave( Session s )
    {
        Query q = s.createQuery( "from ServicesSettingsImpl" );
        for ( Iterator iter = q.iterate() ; iter.hasNext() ; ) {
            ServicesSettingsImpl settings = (ServicesSettingsImpl)iter.next();
            s.delete( settings );
        }
    }
}

class DynamicDnsSettingsDataSaver extends DataSaver<DynamicDNSSettings>
{
    private final DynamicDNSSettings newData;
    public DynamicDnsSettingsDataSaver( LocalUvmContext local, DynamicDNSSettings newData )
    {
        super( local );
        this.newData = newData;
    }

    protected void preSave( Session s )
    {
        Query q = s.createQuery( "from DynamicDNSSettings ds where ds.id != :id" );

        /* Don't delete the new settings object */
        Long settingsId = newData.getId();

        q.setParameter( "id", (( settingsId != null ) ? settingsId : 0 ));
        for ( Iterator iter = q.iterate() ; iter.hasNext() ; ) {
            DynamicDNSSettings settings = (DynamicDNSSettings)iter.next();

            s.delete( settings );
        }
    }

}

