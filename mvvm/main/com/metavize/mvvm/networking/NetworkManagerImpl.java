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

import java.util.List;

import java.net.Inet4Address;

import org.apache.log4j.Logger;

import org.hibernate.Query;
import org.hibernate.Session;

import com.metavize.jnetcap.Netcap;
import com.metavize.jnetcap.InterfaceData;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.NetworkManager;
import com.metavize.mvvm.IntfEnum;

import com.metavize.mvvm.argon.ArgonException;
import com.metavize.mvvm.argon.IntfConverter;
import com.metavize.mvvm.util.TransactionWork;

import com.metavize.mvvm.tran.ValidateException;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.script.ScriptWriter;
import com.metavize.mvvm.tran.script.ScriptRunner;

/* XXX This shouldn't be public, but it has to for now for the same reasons that the argon manager is */
public class NetworkManagerImpl implements NetworkManager
{
    private static NetworkManagerImpl INSTANCE = null;
    
    private static final String ETC_INTERFACES_FILE = "/etc/network/interfaces";
    static final String ETC_RESOLV_FILE     = "/etc/resolv.conf";
    
    private static final Logger logger = Logger.getLogger( NetworkManagerImpl.class );

    private static final String BUNNICULA_BASE = System.getProperty( "bunnicula.home" );

    /* Script to run whenever the interfaces should be reconfigured */
    private static final String NET_CONFIGURE_SCRIPT = BUNNICULA_BASE + "/networking/configure";
    
    /* Script to run whenever the iptables should be updated */
    private static final String IPTABLES_SCRIPT      = BUNNICULA_BASE + "/networking/rule-generator";

    /* Script to run renew the DHCP lease */
    private static final String DHCP_RENEW_SCRIPT    = BUNNICULA_BASE + "/networking/dhcp-renew";

    /* Inidicates whether or not the networking manager has been initialized */
    private boolean isInitialized = false;

    /* the netcap  */
    private final Netcap netcap = Netcap.getInstance();

    /* The rule generator */
    private final RuleManager ruleManager = RuleManager.getInstance();

    /* Flag to indicate when the MVVM has been shutdown */
    private boolean isShutdown = false;

    private NetworkSettings settings = new NetworkSettings();
    
    private NetworkManagerImpl()
    {
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
    }

    public BasicNetworkSettings getBasicNetworkSettings()
    {
        return NetworkUtilPriv.getPrivInstance().toBasicNetworkSettings( this.settings );
    }

    public void setNetworkSettings( BasicNetworkSettings basicSettings )
        throws NetworkException, ValidateException
    {
        setNetworkSettings( NetworkUtilPriv.getPrivInstance().toNetworkSettings( basicSettings ));
    }
    
    public NetworkSettings getNetworkSettings()
    {
        refresh();
        return this.settings;
    }

    /**
     * Save the network settings
     * 1. Validate the settings/
     * 2. Write all of the settings files.
     * 3. Restart networking.
     * 4. Save the settings, done last to guard against the case where
     *    there was an error in one of previous steps and now there are
     *    bogus settings saved into the database.
     */
    public void setNetworkSettings( NetworkSettings newSettings )
        throws NetworkException, ValidateException
    {
        NetworkUtilPriv nup = NetworkUtilPriv.getPrivInstance();
        
        nup.validate( newSettings );
        nup.complete( newSettings );

        /* Write the new configuration files */
        writeFiles( newSettings );
        
        /* Reconfigure networking */
        restartNetworking();

        /* Update the address of the box */
        zUpdateAddress( newSettings );

        saveSettings( newSettings );
    }

    /* Get the public HTTPS port, if this is disabled, then what ??? */
    public int getPublicHttpsPort()
    {
        /* !!!!!!!!!!!!! */
        return 443;
    }
    
    public BasicNetworkSettings renewDhcpLease() throws NetworkException
    {
        /* Dangerous to assume the first space exists */
        List<NetworkSpace> networkSpaceList = (List<NetworkSpace>)this.settings.getNetworkSpaceList();
        
        if ( networkSpaceList.size() < 1 ) {
            throw new NetworkException( "There must be at least one network space" );
        }

        NetworkSpace space = networkSpaceList.get( 0 );
        
        renewDhcpLease( space, true );
        
        return getBasicNetworkSettings();
    }

    /* Renew the DHCP address for a network space. */
    public NetworkSpace renewDhcpLease( NetworkSpace space, boolean isPrimary ) throws NetworkException
    {
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

        /* Return the network space. */
        return space;
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

    public void refresh()
    {
        logger.error( "Refresh is very much totally busted." );
    }

    /* Called when the address changes, eg after saving settings or when pump updates the address. */
    public void updateAddress() throws NetworkException
    {
        zUpdateAddress( this.settings );
    }

    /* The zUpdateSettings is used to possibly update settings that haven't been written to the 
     * database yet.  The z is just so that the objects settings object and the 
     * one being updated don't get confused */
    synchronized private void zUpdateAddress( NetworkSettings zSettings ) throws NetworkException
    {
        /* Update the rules */
        generateRules();
        IntfConverter ic = IntfConverter.getInstance();

        Netcap.updateAddress();

        /* Iterate all of the network spaces looking for DHCP addresses */
        for ( NetworkSpace space : (List<NetworkSpace>)zSettings.getNetworkSpaceList()) {
            if ( space.getIsDhcpEnabled()) {
                /* Grab the new DHCP address */
                try {
                    /* Just grab the address of any one of the devices in the bridge */
                    Interface intf = (Interface)space.getInterfaceList().get( 0 );
                    List<InterfaceData> dataList = 
                        netcap.getInterfaceData( ic.argonIntfToString( intf.getArgonIntf()));
                    
                    /* The DHCP address is the first address in the list */
                    /* XXX this whole thing is  kind of magical */
                    if ( dataList.size() < 1 ) {
                        /* No interfaces addresses, shouldn't happen, but have to deal with it */
                        logger.error( "There are no addresses for [" + space + "], using empty address" );
                        space.setDhcpStatus( new DhcpStatus( IPNetwork.getEmptyNetwork()));
                    } else {
                        InterfaceData dhcp = dataList.get( 0 );
                        IPaddr address = new IPaddr((Inet4Address)dhcp.getAddress());
                        IPaddr netmask = new IPaddr((Inet4Address)dhcp.getNetmask());
                        IPaddr defaultRoute = null;
                        IPaddr dns1 = null;
                        IPaddr dns2 = null;

                        if ( space.isPrimarySpace()) {
                            defaultRoute = new IPaddr((Inet4Address)Netcap.getGateway());
                            List<IPaddr> dnsServers = NetworkUtilPriv.getPrivInstance().getDnsServers();
                            switch ( dnsServers.size()) {
                            case 0:
                                break;
                            case 1:
                                dns1 = dnsServers.get( 0 );
                                break;
                            case 2:
                                dns1 = dnsServers.get( 0 );
                                dns2 = dnsServers.get( 1 );
                                break;
                            }
                        }
                        
                        space.setDhcpStatus( new DhcpStatus( address, netmask, defaultRoute, dns1, dns2 ));
                    }
                    
                } catch ( Exception e ) {
                    logger.error( "Exception retrieving DHCP address for [" + space + "], " +
                                  "setting to the empty network", e );
                    space.setDhcpStatus( new DhcpStatus( IPNetwork.getEmptyNetwork()));
                }
            }
        }
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

    /** Private methods */

    private void writeFiles( NetworkSettings newSettings ) throws NetworkException
    {
        try {
            NetworkUtilPriv.getPrivInstance().complete( newSettings );
            writeEtcFiles( newSettings );
        } catch ( ArgonException e ) {
            logger.error( "Unable to save network settings" );
        }
    }

    private void writeEtcFiles( NetworkSettings newSettings ) throws NetworkException, ArgonException
    {
        writeInterfaces( newSettings );
        writeResolvConf( newSettings );
    }

    /* This is for /etc/network/interfaces interfaces */
    private void writeInterfaces( NetworkSettings newSettings ) throws NetworkException, ArgonException
    {
        /* This is a script writer customized to generate etc interfaces files */
        InterfacesScriptWriter isw = new InterfacesScriptWriter( newSettings );
        isw.addNetworkSettings();
        isw.writeFile( ETC_INTERFACES_FILE );
    }

    private void writeResolvConf( NetworkSettings newSettings )
    {
        ResolvScriptWriter rsw = new ResolvScriptWriter( newSettings );
        rsw.addNetworkSettings();
        rsw.writeFile( ETC_RESOLV_FILE );
    }

    private void restartNetworking() throws NetworkException
    {
        try {
            ScriptRunner.getInstance().exec( NET_CONFIGURE_SCRIPT );
        } catch ( Exception e ) {
            logger.warn( "Error renewing DHCP address", e );
            throw new NetworkException( "Unable to renew the DHCP lease" );
        }
    }

    private boolean loadSettings()
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery( "from NetworkSettings" );
                    /* !!!!! Delete results if there are multiple instances */
                    NetworkManagerImpl.this.settings = (NetworkSettings)q.uniqueResult();
                    return true;
                }

                public Object getResult() { return null; }
            };

        MvvmContextFactory.context().runTransaction( tw );

        return ( this.settings != null );
    }

    private void saveSettings( final NetworkSettings newSettings )
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(newSettings);
                    NetworkManagerImpl.this.settings = newSettings;
                    return true;
                }
                
                public Object getResult() { return null; }
            };
        MvvmContextFactory.context().runTransaction( tw );
    }

    private void initPriv() throws Exception
    {
        /* Grab the settings, loadSettings returns false if it couldn't load settings from the database. */
        boolean hasSettings = loadSettings();
        if ( !hasSettings ) {
            /* The settings could not be loaded from the database, need to build them */
            Converter converter = new Converter();
            
            NetworkSettings newSettings = converter.upgradeSettings();
            
            saveSettings( newSettings );
        } 
        
        NetworkUtilPriv.getPrivInstance().complete( this.settings );
            
        if ( logger.isInfoEnabled()) {
            logger.info( "Loaded the following network settings is-new[" + !hasSettings + "]" );
            logger.info( "DNS: [" + this.settings.getDns1() + "] [" + this.settings.getDns2() + "]" );
            logger.info( "Default Route: [" + this.settings.getDefaultRoute());
            logger.info( "Hostname: " + this.settings.getHostname());
            logger.info( "Public Address: " + this.settings.getPublicAddress());
            logger.info( "Number of network spaces: " + this.settings.getNetworkSpaceList().size());
            
            for ( NetworkSpace space : (List<NetworkSpace>)this.settings.getNetworkSpaceList()) {
                logger.info( "Network space: DHCP[" + space.getIsDhcpEnabled() + "]" );
                
                for ( IPNetworkRule rule : (List<IPNetworkRule>)space.getNetworkList()) {
                    logger.info( "Network: " + rule.getIPNetwork());
                }
                
                logger.info( space.getDhcpStatus());
            }
        }
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
