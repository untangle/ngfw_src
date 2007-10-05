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
package com.untangle.node.router;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import com.untangle.node.token.TokenAdaptor;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.UvmState;
import com.untangle.uvm.localapi.SessionMatcher;
import com.untangle.uvm.localapi.SessionMatcherFactory;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.networking.IPNetwork;
import com.untangle.uvm.networking.IPNetworkRule;
import com.untangle.uvm.networking.LocalNetworkManager;
import com.untangle.uvm.networking.NetworkException;
import com.untangle.uvm.networking.NetworkSettingsListener;
import com.untangle.uvm.networking.NetworkSpace;
import com.untangle.uvm.networking.NetworkSpacesSettings;
import com.untangle.uvm.networking.SetupState;
import com.untangle.uvm.networking.internal.NetworkSpaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.uvm.networking.internal.ServicesInternalSettings;
import com.untangle.uvm.node.AddressValidator;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeContextSwitcher;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.NodeStartException;
import com.untangle.uvm.node.NodeState;
import com.untangle.uvm.node.NodeStopException;
import com.untangle.uvm.util.DataLoader;
import com.untangle.uvm.util.DataSaver;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.MPipe;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import org.apache.log4j.Logger;

public class RouterImpl extends AbstractNode implements Router
{
    private final RouterEventHandler handler;
    private final RouterSessionManager sessionManager;
    private final SettingsManager settingsManager;
    final RouterStatisticManager statisticManager;
    private final DhcpMonitor dhcpMonitor;
    /* Done with an inner class so the GUI doesn't freak out about not
     * having the NetworkSettingsListener class */
    private final SettingsListener listener;
    private final PhoneBookAssistant assistant;

    /* Indicate whether or not the node is starting */

    private final SoloPipeSpec natPipeSpec;
    private final SoloPipeSpec natFtpPipeSpec;

    private final PipeSpec[] pipeSpecs;

    private final EventLogger<LogEvent> eventLogger;

    /** Used to turn on network spaces if the appliances is on, otherwise, network
     * spaces are not turned on at startup. */
    private boolean isUpgrade = false;

    private final Logger logger = Logger.getLogger( RouterImpl.class );

    public RouterImpl()
    {
        this.handler          = new RouterEventHandler(this);
        this.sessionManager   = new RouterSessionManager(this);
        this.statisticManager = new RouterStatisticManager(getNodeContext());
        this.settingsManager  = new SettingsManager();
        this.dhcpMonitor      = new DhcpMonitor( this, LocalUvmContextFactory.context());
        this.listener         = new SettingsListener();
        this.assistant        = new PhoneBookAssistant( this.dhcpMonitor );

        /* Have to figure out pipeline ordering, this should always next
         * to towards the outside */
        natPipeSpec = new SoloPipeSpec
            ("nat", this, this.handler, Fitting.OCTET_STREAM, Affinity.CLIENT,
             SoloPipeSpec.MAX_STRENGTH - 1);

        /* This subscription has to evaluate after NAT */
        natFtpPipeSpec = new SoloPipeSpec
            ("nat-ftp", this, new TokenAdaptor(this, new RouterFtpFactory(this)),
             Fitting.FTP_TOKENS, Affinity.SERVER, 0);

        pipeSpecs = new SoloPipeSpec[] { natPipeSpec, natFtpPipeSpec };

        NodeContext tctx = getNodeContext();
        eventLogger = EventLoggerFactory.factory().getEventLogger(tctx);

        SimpleEventFilter ef = new RouterRedirectFilter();
        eventLogger.addSimpleEventFilter(ef);
    }

    public RouterCommonSettings getRouterSettings()
    {
        /* Get the settings from Network Spaces (The only state in the node is the setup state) */
        LocalNetworkManager nm = getNetworkManager();

        SetupState state = getSetupState();

        NetworkSpacesSettings network = nm.getNetworkSettings();
        NetworkSpacesInternalSettings networkInternal = nm.getNetworkInternalSettings();
        ServicesInternalSettings servicesInternal = nm.getServicesInternalSettings();

        if ( state.equals( SetupState.BASIC )) {
            RouterCommonSettings common = settingsManager.
                toBasicSettings( this.getTid(), networkInternal, servicesInternal );

            nm.updateLeases( common );
            return common;
        } else if ( state.equals( SetupState.ADVANCED )) {
            RouterCommonSettings common =
                settingsManager.toAdvancedSettings( network, networkInternal, servicesInternal );

            nm.updateLeases( common );
            return common;
        }

        logger.error( "Invalid state: [" + state + "] using basic" );

        return settingsManager.toBasicSettings( this.getTid(), networkInternal, servicesInternal );
    }

    public void setRouterSettings( RouterCommonSettings settings ) throws Exception
    {
        /* Remove all of the non-static addresses before saving */

        /* Validate the settings */
        try {
            settings.validate();
        }
        catch ( Exception e ) {
            logger.error("Invalid NAT settings", e);
            throw e;
        }

        LocalNetworkManager networkManager = getNetworkManager();

        /* Integrate the settings from the internal network and the ones from the user */
        NetworkSpacesSettings networkSettings = networkManager.getNetworkSettings();

        NetworkSpacesSettings newNetworkSettings = null;

        try {
            SetupState state = settings.getSetupState();
            if ( state.equals( SetupState.BASIC )) {
                newNetworkSettings = this.settingsManager.
                    toNetworkSettings( networkSettings, (RouterBasicSettings)settings );
            } else if ( state.equals( SetupState.ADVANCED )) {
                newNetworkSettings = this.settingsManager.
                    toNetworkSettings( networkSettings, (RouterAdvancedSettings)settings );
            } else {
                throw new Exception( "Illegal setup state: " + state );
            }

        } catch ( Exception e ) {
            logger.error( "Unable to convert the settings objects.", e );
            throw e;
        }

        boolean isEnabled = ( getRunState() == NodeState.RUNNING );
        /* This isn't necessary, (the state should carry over), but
         * just in case. */
        newNetworkSettings.setIsEnabled( isEnabled );

        /* Indicate that you are not in setup wizard mode anymore */
        newNetworkSettings.setHasCompletedSetup( true );

        try {
            /* Have to reconfigure the network before configure the
             * services settings */
            networkManager.setNetworkSettings( newNetworkSettings, isEnabled );
            networkManager.setServicesSettings( settings );

            /* Trigger an update address to regenerate the iptables rules */
            networkManager.updateAddress();
        } catch ( Exception e ) {
            logger.error( "Could not reconfigure the network", e );
            throw e;
        }
    }

    /* Reinitialize the settings to basic nat */
    public void resetBasic() throws Exception
    {
        /* This shouldn't fail */

        /* Get the settings from Network Spaces (The only state in the node is the setup state) */
        LocalNetworkManager nm = getNetworkManager();

        NetworkSpacesSettings newSettings =
            this.settingsManager.resetToBasic( getTid(), nm.getNetworkSettings());

        /* Only reconfigure if the node is enabled */
        nm.setNetworkSettings( newSettings, getRunState() == NodeState.RUNNING );
    }

    /* Convert the basic settings to advanced Network Spaces */
    public void switchToAdvanced() throws Exception
    {
        /* Get the settings from Network Spaces (The only state in the node is the setup state) */
        LocalNetworkManager nm = getNetworkManager();

        NetworkSpacesSettings newSettings = this.settingsManager.basicToAdvanced( nm.getNetworkSettings());

        /* Only reconfigure if the node is enabled */
        nm.setNetworkSettings( newSettings, getRunState() == NodeState.RUNNING );
    }

    public SetupState getSetupState()
    {
        SetupState state = getNetworkSettings().getSetupState();
        if ( state == null ) {
            logger.error( "NULL State" );
            state = SetupState.BASIC;
        }

        return state;
    }

    public EventManager<LogEvent> getEventManager()
    {
        return eventLogger;
    }

    // package protected methods ----------------------------------------------

    RouterEventHandler getHandler()
    {
        return handler;
    }

    MPipe getRouterMPipe()
    {
        return natPipeSpec.getMPipe();
    }

    MPipe getRouterFtpPipeSpec()
    {
        return natFtpPipeSpec.getMPipe();
    }

    // AbstractNode methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    public void initializeSettings()
    {
        logger.info("Initializing Settings...");

        RouterBasicSettings settings = settingsManager.getDefaultSettings( this.getTid());

        /* Disable everything */

        /* deconfigure the event handle and the dhcp manager */
        dhcpMonitor.stop();

        try {
            setRouterSettings( settings );
            // Handler doesn't need to be deconfigured at initialization.
            // handler.deconfigure();
        } catch( Exception e ) {
            logger.error( "Unable to set Router Settings", e );
        }

        /* Stop the statistics manager */
        statisticManager.stop();
    }

    @Override
    protected void postInit(String[] args) throws NodeException
    {
        super.postInit( args );

        /* Register a listener, this should hang out until the node is removed dies. */
        getNetworkManager().registerListener( this.listener );

        LocalUvmContextFactory.context().localPhoneBook().registerAssistant( this.assistant );

        /* Check if the settings have been upgraded yet */
        DataLoader<RouterSettingsImpl> natLoader = new DataLoader<RouterSettingsImpl>( "RouterSettingsImpl",
                                                                                 getNodeContext());

        RouterSettingsImpl settings = natLoader.loadData();

        if ( settings == null ) {

        } else {
            /* In deprecated, mode, update and save new settings */
            SetupState state = settings.getSetupState();
            if ( state.equals( SetupState.NETWORK_SHARING )) {
                logger.info( "Settings are in the deprecated mode, upgrading settings" );
                settings.setSetupState( SetupState.BASIC );

                /* Save the new Settings */
                try {
                    setRouterSettings( settings );
                } catch ( Exception e ) {
                    logger.error( "Unable to set upgrade nat settings", e );
                }

                /* Indicate to enable network spaces when then devices powers on */
                this.isUpgrade = true;
            } else if ( state.equals( SetupState.WIZARD )) {
                postInitWizard();

                /* Indicate to enable network spaces when then devices powers on */
                this.isUpgrade = true;
            }  else {
                logger.info( "Settings are in [" + settings.getSetupState() +"]  mode, ignoring." );
            }

            /* If upgrading change the setting to basic mode, this just means they
             * won't be upgraded again.*/
            if ( this.isUpgrade ) {
                /* Change to basic mode */
                settings.setSetupState( SetupState.BASIC );
                DataSaver<RouterSettingsImpl> dataSaver = new DataSaver<RouterSettingsImpl>( getNodeContext());
                dataSaver.saveData( settings );
            }
        }
    }

    protected void preStart() throws NodeStartException
    {
        LocalUvmContext context = LocalUvmContextFactory.context();
        UvmState state = context.state();
        LocalNetworkManager networkManager = getNetworkManager();

        /* Enable the network settings */
        if ( state.equals( UvmState.RUNNING ) || this.isUpgrade ) {
            logger.debug( "enabling network spaces settings because user powered on nat or upgrade." );

            try {
                networkManager.enableNetworkSpaces();
            } catch ( Exception e ) {
                throw new NodeStartException( "Unable to enable network spaces", e );
            }

            this.isUpgrade = false;
        } else {
            logger.debug( "not enabling network spaces settings at startup" );
        }

        NetworkSpacesInternalSettings networkSettings = getNetworkSettings();
        ServicesInternalSettings servicesSettings = getServicesSettings();

        try {
            configureDhcpMonitor( servicesSettings.getIsDhcpEnabled());
            this.assistant.configure( servicesSettings );
            this.handler.configure( networkSettings );
            networkManager.startServices();
        } catch( NodeException e ) {
            logger.error( "Could not configure the handler.", e );
            throw new NodeStartException( "Unable to configure the handler" );
        } catch( NetworkException e ) {
            logger.error( "Could not start services.", e );
            throw new NodeStartException( "Unable to configure the handler" );
        }

        statisticManager.start();
    }

    protected void postStart()
    {
        /* Kill all active sessions */
        shutdownMatchingSessions();
    }

    protected void postStop() throws NodeStopException
    {
        /* Kill all active sessions */
        shutdownMatchingSessions();

        LocalUvmContext context = LocalUvmContextFactory.context();

        UvmState state = context.state();

        LocalNetworkManager networkManager = context.networkManager();

        /* Only stop the services if the box isn't going down (the user turned off the appliance) */
        if ( state.equals( UvmState.RUNNING ))  {
            logger.debug( "Disabling services since user turned off network spaces." );
            networkManager.stopServices();
        }

        dhcpMonitor.stop();

        statisticManager.stop();

        /* deconfigure the event handle */
        handler.deconfigure();

        /* Deconfigure the network spaces */
        /* Only stop the services if the box isn't going down (the user turned off the appliance) */
        if ( state.equals( UvmState.RUNNING )) {
            logger.debug( "Disabling network spaces since user turned off network spaces." );
            try {
                networkManager.disableNetworkSpaces();
            } catch ( Exception e ) {
                logger.error( "Unable to disable network spaces", e );
            }
        }
    }

    @Override protected void postDestroy() throws NodeException
    {
        /* Deregister the network settings listener */
        getNetworkManager().unregisterListener( this.listener );

        LocalUvmContextFactory.context().localPhoneBook().unregisterAssistant( this.assistant );
    }

    public void networkSettingsEvent( ) throws NodeException
    {
        logger.info("networkSettingsEvent");

        /* ????, what goes here. Configure the handler */

        /* Retrieve the new settings from the network manager */
        LocalNetworkManager nm = getNetworkManager();
        NetworkSpacesInternalSettings networkSettings = nm.getNetworkInternalSettings();
        ServicesInternalSettings servicesSettings = nm.getServicesInternalSettings();

        if ( getRunState() == NodeState.RUNNING ) {
            /* Have to configure DHCP before the handler, this automatically starts the dns server */
            configureDhcpMonitor( servicesSettings.getIsDhcpEnabled());
            this.handler.configure( networkSettings );
        } else {
            nm.stopServices();
            this.handler.deconfigure();
        }

        this.assistant.configure( servicesSettings );
    }


    private void updateToCurrent( RouterSettings settings )
    {
        if (settings == null) {
            logger.error("NULL Router Settings");
        } else {
            logger.info( "Update Settings Complete" );
        }
    }

    /* Kill all sessions when starting or stopping this node */
    protected SessionMatcher sessionMatcher()
    {
        return SessionMatcherFactory.getAllInstance();
    }

    void log(LogEvent le)
    {
        eventLogger.log(le);
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getRouterSettings();
    }

    public void setSettings(Object settings) throws Exception
    {
        setRouterSettings((RouterCommonSettings)settings);
    }

    private void configureDhcpMonitor( boolean isDhcpEnabled )
    {
        if ( isDhcpEnabled ) dhcpMonitor.start();
        else dhcpMonitor.stop();
    }

    private void postInitWizard()
    {
        logger.info( "Settings are not setup yet, using defaults for setup wizard" );

        try {
            LocalNetworkManager networkManager = getNetworkManager();

            /* Integrate the settings from the internal network and the ones from the user */
            NetworkSpacesSettings networkSettings = networkManager.getNetworkSettings();

            /* Get the default settings, save them, and indicate
             * to turn on network spaces at startup. */
            RouterBasicSettings defaultSettings =
                this.settingsManager.getDefaultSettings( this.getTid());

            defaultSettings.setNatInternalAddress( RouterUtil.SETUP_INTERNAL_ADDRESS );
            defaultSettings.setNatInternalSubnet( RouterUtil.SETUP_INTERNAL_SUBNET );
            defaultSettings.setDhcpStartAndEndAddress( RouterUtil.SETUP_DHCP_START, RouterUtil.SETUP_DHCP_END );

            NetworkSpacesSettings newNetworkSettings = this.settingsManager.
                toNetworkSettings( networkSettings, (RouterBasicSettings)defaultSettings );

            newNetworkSettings.setHasCompletedSetup( false );

            /* Change the primary space to DHCP, this way it only happens once at startup. */
            NetworkSpace primary = newNetworkSettings.getNetworkSpaceList().get( 0 );
            primary.setIsDhcpEnabled( true );
            /* Clear the list of aliases */
            primary.setNetworkList( new LinkedList<IPNetworkRule>());

            networkManager.setNetworkSettings( newNetworkSettings, false );
            networkManager.setServicesSettings( defaultSettings );

            /* Trigger an update address to regenerate the iptables rules */
            networkManager.updateAddress();
        } catch ( Exception e ) {
            logger.error( "Unable to set wizard nat settings", e );
        }
    }

    /**
     * Returns true if the edgeguard detects that there is a Router on the outside
     * of it that is NATing traffic
     */
    private boolean isRouterDetected()
    {
        NetworkSpacesInternalSettings networkSettings = getNetworkSettings();

        /* Nothing to check, settings are null */
        if ( networkSettings == null ) {
            logger.warn( "Unable to detect router, null network settings" );
            return false;
        }

        List<NetworkSpaceInternal> networkSpaceList = networkSettings.getNetworkSpaceList();

        if ( networkSpaceList == null ) {
            logger.warn( "Unable to detect router, null network space list" );
            return false;
        }

        if ( networkSpaceList.size() == 0 ) {
            logger.warn( "Unable to detect router, No network spaces" );
            return false;
        }

        NetworkSpaceInternal space = networkSpaceList.get( 0 );

        IPNetwork network = space.getPrimaryAddress();
        if ( network == null || IPNetwork.getEmptyNetwork().equals( network )) {
            logger.warn( "Unable to detect router, NULL or empty network on primary space." );
            return false;
        }

        /* Verify that the address was retrieved using DHCP */
        if ( !space.getIsDhcpEnabled()) {
            logger.debug( "DHCP is disabled, assuming an external router is not in place." );
            return false;
        }

        /* If not in a private network, this is a public address */
        InetAddress publicAddress = network.getNetwork().getAddr();
        if ( !AddressValidator.getInstance().isInPrivateNetwork( network.getNetwork().getAddr())) {
            logger.debug( "Detected public address for '"+ publicAddress.getHostAddress() + "'" );
            return false;
        }

        logger.debug( "Detected private address for '"+ publicAddress.getHostAddress() + "'" );
        return true;
    }

    private LocalNetworkManager getNetworkManager()
    {
        return LocalUvmContextFactory.context().networkManager();
    }
    private NetworkSpacesInternalSettings getNetworkSettings()
    {
        return getNetworkManager().getNetworkInternalSettings();
    }

    private ServicesInternalSettings getServicesSettings()
    {
        return getNetworkManager().getServicesInternalSettings();
    }

    RouterSessionManager getSessionManager()
    {
        return sessionManager;
    }

    class SettingsListener
        implements NetworkSettingsListener, NodeContextSwitcher.Event<NetworkSpacesInternalSettings>
    {
        /* Use this to automatically switch context */
        private final NodeContextSwitcher<NetworkSpacesInternalSettings> tcs;

        /* This are the settings passed in by the network settings */
        private NetworkSpacesInternalSettings settings;

        SettingsListener()
        {
            tcs = new NodeContextSwitcher<NetworkSpacesInternalSettings>( getNodeContext());
        }

        public void event( NetworkSpacesInternalSettings settings )
        {
            tcs.run( this, settings );
        }

        /* NodeContextSwitcher.Event */
        public void handle( NetworkSpacesInternalSettings settings )
        {
            if ( logger.isDebugEnabled()) logger.debug( "network settings changed:" + settings );
            try {
                networkSettingsEvent();
            } catch( NodeException e ) {
                logger.error( "Unable to reconfigure the NAT node" );
            }
        }
    }
}
