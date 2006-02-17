/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.networking;

import org.apache.log4j.Logger;

import com.metavize.jnetcap.Netcap;

import com.metavize.mvvm.NetworkManager;
import com.metavize.mvvm.IntfEnum;
import com.metavize.mvvm.NetworkingConfiguration;


import com.metavize.mvvm.argon.IntfConverter;
import com.metavize.mvvm.argon.ArgonException;

import com.metavize.mvvm.tran.ValidateException;
import com.metavize.mvvm.tran.script.ScriptWriter;
import com.metavize.mvvm.tran.script.ScriptRunner;

import com.metavize.mvvm.networking.internal.ServicesInternalSettings;
import com.metavize.mvvm.networking.internal.NetworkSpacesInternalSettings;
import com.metavize.mvvm.networking.internal.NetworkSpaceInternal;
import com.metavize.mvvm.networking.internal.RouteInternal;
import com.metavize.mvvm.networking.internal.InterfaceInternal;

/* XXX This shouldn't be public */
public class NetworkManagerImpl implements NetworkManager
{
    private static NetworkManagerImpl INSTANCE = null;

    static final String ETC_INTERFACES_FILE = "/etc/network/interfaces";
    static final String ETC_RESOLV_FILE = "/etc/resolv.conf";
    
    private static final Logger logger = Logger.getLogger( NetworkManagerImpl.class );

    static final String BUNNICULA_BASE = System.getProperty( "bunnicula.home" );
    static final String BUNNICULA_CONF = System.getProperty( "bunnicula.conf.dir" );

    /* Script to run whenever the interfaces should be reconfigured */
    private static final String NET_CONFIGURE_SCRIPT = BUNNICULA_BASE + "/networking/configure";
    
    /* Script to run whenever the iptables should be updated */
    private static final String IPTABLES_SCRIPT      = BUNNICULA_BASE + "/networking/rule-generator";

    /* Script to run renew the DHCP lease */
    private static final String DHCP_RENEW_SCRIPT    = BUNNICULA_BASE + "/networking/dhcp-renew";

    /* A flag for devel environments, used to determine whether or not 
     * the etc files actually are written, this enables/disables reconfiguring networking */
    private boolean saveSettings = true;
    
    /* Inidicates whether or not the networking manager has been initialized */
    private boolean isInitialized = false;

    /* Manager for the iptables rules */
    private RuleManager ruleManager;

    /* Manager for the DHCP/DNS server */
    private DhcpManager dhcpManager;

    /* Converter to create the initial networking configuration object if
     * network spaces has never been executed before */
    private NetworkConfigurationLoader networkConfigurationLoader;

    /** The nuts and bolts of networking, the real bits of panther.  this my friend
     * should never be null */
    private NetworkSpacesInternalSettings settings = null;

    /** The configuration for the DHCP/DNS Server */
    private ServicesInternalSettings servicesSettings = null;

    /* These are the "networking" settings that aren't related to the nuts and bolts of
     * network spaces.  Things like SSH support */
    private RemoteSettings remote = null;

    /* the netcap  */
    private final Netcap netcap = Netcap.getInstance();

    /* Flag to indicate when the MVVM has been shutdown */
    private boolean isShutdown = false;

    private NetworkManagerImpl()
    {
        this.ruleManager = RuleManager.getInstance();
        this.networkConfigurationLoader = NetworkConfigurationLoader.getInstance();
        this.dhcpManager  = new DhcpManager();
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

    public NetworkingConfiguration getNetworkingConfiguration()
    {
        return NetworkUtilPriv.getPrivInstance().toConfiguration( this.settings, this.remote );
    }
    
    public synchronized void setNetworkingConfiguration( NetworkingConfiguration configuration )
        throws NetworkException, ValidateException
    {
        setNetworkSettings( NetworkUtilPriv.getPrivInstance().toInternal( configuration, this.settings ));
        setRemoteSettings( configuration );
    }

    public NetworkSpacesSettings getNetworkSettings()
    {
        return NetworkUtilPriv.getPrivInstance().toSettings( this.settings );
    }

    public synchronized void setNetworkSettings( NetworkSpacesSettings settings )
        throws NetworkException, ValidateException
    {
        logger.debug( "Loading the new network settings: " + settings );
        setNetworkSettings( NetworkUtilPriv.getPrivInstance().toInternal( settings ));
    }

    private synchronized void setNetworkSettings( NetworkSpacesInternalSettings newSettings )
        throws NetworkException, ValidateException
    {
        logger.debug( "Loading the new network settings: " + newSettings );
        /* XXXX implement me */
        // throw new IllegalStateException( "implement me" );

        /* Write the settings */
        writeConfiguration( newSettings );
        
        this.settings = newSettings;
    }

    public synchronized void setRemoteSettings( RemoteSettings remote )
        throws NetworkException
    {
        /* XXXXXXXXX Implement me */
        this.remote = remote;
    }

    public NetworkSpacesInternalSettings getNetworkInternalSettings()
    {
        return this.settings;
    }
    
    /* XXXX This is kind of busted since you can't change the services on/off switch from here */
    public synchronized void setServicesSettings( ServicesSettings servicesSettings )
        throws NetworkException
    {
        setServicesSettings( servicesSettings, servicesSettings );
    }

    public synchronized void setServicesSettings( DhcpServerSettings dhcp, DnsServerSettings dns )
        throws NetworkException
    {
        logger.debug( "Loading the new dhcp settings: " + dhcp );
        logger.debug( "Loading the new dns settings: " + dns );

        this.servicesSettings = NetworkUtilPriv.getPrivInstance().toInternal( settings, dhcp, dns );
        this.dhcpManager.configure( this.servicesSettings );
        // !!!!!! this.dhcpManager.startDnsMasq();
    }

    public ServicesInternalSettings getServicesInternalSettings()
    {
        return this.servicesSettings;
    }


    public synchronized void startServices() throws NetworkException
    {
        this.dhcpManager.configure( servicesSettings );
        // !!!!!!! this.dhcpManager.startDnsMasq();
    }

    public synchronized void stopServices()
    {
        this.dhcpManager.deconfigure();
    }

    public synchronized void disableNetworkSpaces()
    {
        
    }

    public synchronized void enableNetworkSpaces()
    {
        
    }

    /* Get the external HTTPS port */
    public int getPublicHttpsPort()
    {
        /* !!!!!!!!!!!!! */
        return 443;
    }
    
    /* Renew the DHCP address and return a new network settings with the updated address */
    public synchronized NetworkingConfiguration renewDhcpLease() throws NetworkException
    {
        renewDhcpLease( 0 );
        
        return getNetworkingConfiguration();
    }

    /* Renew the DHCP address for a network space. */
    public synchronized DhcpStatus renewDhcpLease( int index ) throws NetworkException
    {
        if (( index < 0 ) || ( index >= this.settings.getNetworkSpaceList().size())) {
            throw new NetworkException( "There isn't a network space at index " + index );
        }

        boolean isPrimary = ( index == 0 );
        
        NetworkSpaceInternal space = this.settings.getNetworkSpaceList().get( index );
        
        if ( !space.getIsDhcpEnabled()) {
            throw new NetworkException( "DHCP is not enabled on this network space." );
        }

        /* Renew the address */
        try {
            String flags = "";
            
            if ( !isPrimary ) flags = InterfacesScriptWriter.DHCP_FLAG_ADDRESS_ONLY;
            
            ScriptRunner.getInstance().exec( DHCP_RENEW_SCRIPT, space.getDeviceName(), 
                                             String.valueOf( space.getIndex()), flags );
        } catch ( Exception e ) { 
            logger.warn( "Error renewing DHCP address", e );
            throw new NetworkException( "Unable to renew the DHCP lease" );
        }

        /* Update the address and generate new rules */
        updateAddress();

        /* Get the new space (the settings get updated by updateAddress) */
        if (( index < 0 ) || ( index >= this.settings.getNetworkSpaceList().size())) {
            throw new NetworkException( "There is no longer a network space at index " + index );
        }
        
        space = this.settings.getNetworkSpaceList().get( index );
        
        if ( !space.getIsDhcpEnabled()) {
            throw new NetworkException( "DHCP is no longer enabled on this network space." );
        }
        
        IPNetwork network = space.getPrimaryAddress();
        if ( !isPrimary ) return new DhcpStatus( network.getNetwork(), network.getNetmask());
        
        return new DhcpStatus( network.getNetwork(), network.getNetmask(), this.settings.getDefaultRoute(),
                               this.settings.getDns1(), this.settings.getDns2());
    }

    /* Retrieve the enumeration of all of the active interfaces */
    public IntfEnum getIntfEnum()
    {
        return null;
    }

    public String getHostname()
    {
        return this.settings.getHostname();
    }

    public String getPublicAddress()
    {
        return this.settings.getPublicAddress();
    }
    
    public void updateAddress()
    {
        
    }

    public void disableDhcpForwarding()
    {
        this.ruleManager.dhcpEnableForwarding( false );
    }
    
    public void enableDhcpForwarding()
    {
         this.ruleManager.dhcpEnableForwarding( true );
    }

    /* This relic really should go away.  In production environments, none of the
     * interfaces are antisubscribed (this is the way it should be).
     * the antisubscribes are then for specific traffic protocols, such as HTTP, */
    public void subscribeLocalOutside( boolean newValue )
    {
        this.ruleManager.subscribeLocalOutside( newValue );
    }
    
    /** Public (private, only in impl) methods  */
    
    /* Update all of the iptables rules and the inside address database */
    private void generateRules() throws NetworkException
    {
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

    /**** private methods ***/
    private void initPriv() throws NetworkException, ValidateException
    {
        /* !!!! Load settings */
        String saveSettings = System.getProperty( "bunnicula.devel.networking" );

        if ( Boolean.valueOf( saveSettings ) == false ) this.saveSettings = false;
        
        /* If there are no settings, get the settings from the database */
        if ( this.settings == null ) {
            /* Need to create new settings, (The method setNetworkingConfiguration assumes that
             * settings is already set, and cannot be used here) */
            NetworkingConfiguration configuration = networkConfigurationLoader.getNetworkingConfiguration();
                        
            /* Save these settings */
            NetworkSpacesInternalSettings internal = 
                NetworkUtilPriv.getPrivInstance().toInternal( configuration );

            if ( logger.isDebugEnabled()) {
                logger.debug( "Loaded the configuration: \n" + configuration );
                logger.debug( "Converted to: \n" + internal );
            }
            
            /* Save the network settings */
            setNetworkSettings( internal );
            setRemoteSettings( configuration );
        }

        /* Generate rules */
        generateRules();
    }    
    
    private void writeConfiguration( NetworkSpacesInternalSettings newSettings ) throws NetworkException
    {
        
        if ( this.saveSettings == false ) {
            /* Set to a warn, because if this gets emailed out, something has gone terribly awry */
            logger.warn( "Not writing configuration files because the debug property was set" );
            return;
        }

        try {
            writeEtcFiles( newSettings );
        } catch ( ArgonException e ) {
            logger.error( "Unable to write network settings" );
        }
    }

    private void writeEtcFiles( NetworkSpacesInternalSettings newSettings ) 
        throws NetworkException, ArgonException
    {
        writeInterfaces( newSettings );
        writeResolvConf( newSettings );
    }

    /* This is for /etc/network/interfaces interfaces */
    private void writeInterfaces( NetworkSpacesInternalSettings newSettings )
        throws NetworkException, ArgonException
    {
        /* This is a script writer customized to generate etc interfaces files */
        InterfacesScriptWriter isw = new InterfacesScriptWriter( newSettings );
        isw.addNetworkSettings();
        isw.writeFile( ETC_INTERFACES_FILE );
    }

    private void writeResolvConf( NetworkSpacesInternalSettings newSettings )
    {
        /* This is a script writer customized to generate etc resolv.conf files */
        ResolvScriptWriter rsw = new ResolvScriptWriter( newSettings );
        rsw.addNetworkSettings();
        rsw.writeFile( ETC_RESOLV_FILE );
    }

    /* Create a networking manager, this is a first come first serve
     * basis.  The first class to create the network manager gets a
     * networking manager, all other classes get AccessException.  Done
     * this way so only the MvvmContextImpl can create a networking manager
     * and then give out access to those classes (argon) that need it.
     * @throws AccessException - the networking manager has already
     * been initialized. */
    public synchronized static NetworkManagerImpl makeInstance() throws AccessException
    {
        if ( INSTANCE != null ) {
            throw new AccessException( "The networking manager has already been initialized" );
        }

        INSTANCE = new NetworkManagerImpl();

        return INSTANCE;
    }
}
