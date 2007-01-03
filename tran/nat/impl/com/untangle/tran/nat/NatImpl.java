/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.tran.nat;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmLocalContext;
import com.untangle.mvvm.MvvmState;
import com.untangle.mvvm.localapi.SessionMatcher;
import com.untangle.mvvm.localapi.SessionMatcherFactory;
import com.untangle.mvvm.logging.EventLogger;
import com.untangle.mvvm.logging.EventLoggerFactory;
import com.untangle.mvvm.logging.EventManager;
import com.untangle.mvvm.logging.LogEvent;
import com.untangle.mvvm.logging.SimpleEventFilter;
import com.untangle.mvvm.networking.IPNetwork;
import com.untangle.mvvm.networking.IPNetworkRule;
import com.untangle.mvvm.networking.LocalNetworkManager;
import com.untangle.mvvm.networking.NetworkException;
import com.untangle.mvvm.networking.NetworkSettingsListener;
import com.untangle.mvvm.networking.NetworkSpace;
import com.untangle.mvvm.networking.NetworkSpacesSettings;
import com.untangle.mvvm.networking.NetworkUtil;
import com.untangle.mvvm.networking.RemoteSettings;
import com.untangle.mvvm.networking.SetupState;
import com.untangle.mvvm.networking.internal.NetworkSpaceInternal;
import com.untangle.mvvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.mvvm.networking.internal.ServicesInternalSettings;
import com.untangle.mvvm.tapi.AbstractTransform;
import com.untangle.mvvm.tapi.Affinity;
import com.untangle.mvvm.tapi.Fitting;
import com.untangle.mvvm.tapi.MPipe;
import com.untangle.mvvm.tapi.PipeSpec;
import com.untangle.mvvm.tapi.SoloPipeSpec;
import com.untangle.mvvm.tran.AddressValidator;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.tran.TransformContextSwitcher;
import com.untangle.mvvm.tran.TransformException;
import com.untangle.mvvm.tran.TransformStartException;
import com.untangle.mvvm.tran.TransformState;
import com.untangle.mvvm.tran.TransformStopException;
import com.untangle.mvvm.util.DataLoader;
import com.untangle.mvvm.util.DataSaver;
import com.untangle.tran.token.TokenAdaptor;
import org.apache.log4j.Logger;

public class NatImpl extends AbstractTransform implements Nat
{
    private final NatEventHandler handler;
    private final NatSessionManager sessionManager;
    private final SettingsManager settingsManager;
    final NatStatisticManager statisticManager;
    private final DhcpMonitor dhcpMonitor;
    /* Done with an inner class so the GUI doesn't freak out about not
     * having the NetworkSettingsListener class */
    private final SettingsListener listener;
    private final PhoneBookAssistant assistant;

    /* Indicate whether or not the transform is starting */

    private final SoloPipeSpec natPipeSpec;
    private final SoloPipeSpec natFtpPipeSpec;

    private final PipeSpec[] pipeSpecs;

    private final EventLogger<LogEvent> eventLogger;

    /** Used to turn on network spaces if the appliances is on, otherwise, network
     * spaces are not turned on at startup. */
    private boolean isUpgrade = false;

    private final Logger logger = Logger.getLogger( NatImpl.class );

    public NatImpl()
    {
        this.handler          = new NatEventHandler(this);
        this.sessionManager   = new NatSessionManager(this);
        this.statisticManager = new NatStatisticManager(getTransformContext());
        this.settingsManager  = new SettingsManager();
        this.dhcpMonitor      = new DhcpMonitor( this, MvvmContextFactory.context());
        this.listener         = new SettingsListener();
        this.assistant        = new PhoneBookAssistant( this.dhcpMonitor );

        /* Have to figure out pipeline ordering, this should always next
         * to towards the outside */
        natPipeSpec = new SoloPipeSpec
            ("nat", this, this.handler, Fitting.OCTET_STREAM, Affinity.OUTSIDE,
             SoloPipeSpec.MAX_STRENGTH - 1);

        /* This subscription has to evaluate after NAT */
        natFtpPipeSpec = new SoloPipeSpec
            ("nat-ftp", this, new TokenAdaptor(this, new NatFtpFactory(this)),
             Fitting.FTP_TOKENS, Affinity.SERVER, 0);

        pipeSpecs = new SoloPipeSpec[] { natPipeSpec, natFtpPipeSpec };

        TransformContext tctx = getTransformContext();
        eventLogger = EventLoggerFactory.factory().getEventLogger(tctx);

        SimpleEventFilter ef = new NatRedirectFilter();
        eventLogger.addSimpleEventFilter(ef);
    }

    public NatCommonSettings getNatSettings()
    {
        /* Get the settings from Network Spaces (The only state in the transform is the setup state) */
        LocalNetworkManager nm = getNetworkManager();

        SetupState state = getSetupState();

        NetworkSpacesSettings network = nm.getNetworkSettings();
        NetworkSpacesInternalSettings networkInternal = nm.getNetworkInternalSettings();
        ServicesInternalSettings servicesInternal = nm.getServicesInternalSettings();

        if ( state.equals( SetupState.BASIC )) {
            NatCommonSettings common = settingsManager.
                toBasicSettings( this.getTid(), networkInternal, servicesInternal );

            nm.updateLeases( common );
            return common;
        } else if ( state.equals( SetupState.ADVANCED )) {
            NatCommonSettings common =
                settingsManager.toAdvancedSettings( network, networkInternal, servicesInternal );

            nm.updateLeases( common );
            return common;
        }

        logger.error( "Invalid state: [" + state + "] using basic" );

        return settingsManager.toBasicSettings( this.getTid(), networkInternal, servicesInternal );
    }

    public void setNatSettings( NatCommonSettings settings ) throws Exception
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
                    toNetworkSettings( networkSettings, (NatBasicSettings)settings );
            } else if ( state.equals( SetupState.ADVANCED )) {
                newNetworkSettings = this.settingsManager.
                    toNetworkSettings( networkSettings, (NatAdvancedSettings)settings );
            } else {
                throw new Exception( "Illegal setup state: " + state );
            }

        } catch ( Exception e ) {
            logger.error( "Unable to convert the settings objects.", e );
            throw e;
        }

        boolean isEnabled = ( getRunState() == TransformState.RUNNING );
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

        /* Get the settings from Network Spaces (The only state in the transform is the setup state) */
        LocalNetworkManager nm = getNetworkManager();

        NetworkSpacesSettings newSettings =
            this.settingsManager.resetToBasic( getTid(), nm.getNetworkSettings());

        /* Only reconfigure if the transform is enabled */
        nm.setNetworkSettings( newSettings, getRunState() == TransformState.RUNNING );
    }

    /* Convert the basic settings to advanced Network Spaces */
    public void switchToAdvanced() throws Exception
    {
        /* Get the settings from Network Spaces (The only state in the transform is the setup state) */
        LocalNetworkManager nm = getNetworkManager();

        NetworkSpacesSettings newSettings = this.settingsManager.basicToAdvanced( nm.getNetworkSettings());

        /* Only reconfigure if the transform is enabled */
        nm.setNetworkSettings( newSettings, getRunState() == TransformState.RUNNING );
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

    NatEventHandler getHandler()
    {
        return handler;
    }

    MPipe getNatMPipe()
    {
        return natPipeSpec.getMPipe();
    }

    MPipe getNatFtpPipeSpec()
    {
        return natFtpPipeSpec.getMPipe();
    }

    // AbstractTransform methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    public void initializeSettings()
    {
        logger.info("Initializing Settings...");

        NatBasicSettings settings = settingsManager.getDefaultSettings( this.getTid());

        /* Disable everything */

        /* deconfigure the event handle and the dhcp manager */
        dhcpMonitor.stop();

        try {
            setNatSettings( settings );
            // Handler doesn't need to be deconfigured at initialization.
            // handler.deconfigure();
        } catch( Exception e ) {
            logger.error( "Unable to set Nat Settings", e );
        }

        /* Stop the statistics manager */
        statisticManager.stop();
    }

    @Override
    protected void postInit(String[] args) throws TransformException
    {
        super.postInit( args );

        /* Register a listener, this should hang out until the transform is removed dies. */
        getNetworkManager().registerListener( this.listener );

        MvvmContextFactory.context().localPhoneBook().registerAssistant( this.assistant );

        /* Check if the settings have been upgraded yet */
        DataLoader<NatSettingsImpl> natLoader = new DataLoader<NatSettingsImpl>( "NatSettingsImpl",
                                                                                 getTransformContext());

        NatSettingsImpl settings = natLoader.loadData();

        if ( settings == null ) {

        } else {
            /* In deprecated, mode, update and save new settings */
            SetupState state = settings.getSetupState();
            if ( state.equals( SetupState.NETWORK_SHARING )) {
                logger.info( "Settings are in the deprecated mode, upgrading settings" );
                settings.setSetupState( SetupState.BASIC );

                /* Save the new Settings */
                try {
                    setNatSettings( settings );
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
                DataSaver<NatSettingsImpl> dataSaver = new DataSaver<NatSettingsImpl>( getTransformContext());
                dataSaver.saveData( settings );
            }
        }
    }

    protected void preStart() throws TransformStartException
    {
        MvvmLocalContext context = MvvmContextFactory.context();
        MvvmState state = context.state();
        LocalNetworkManager networkManager = getNetworkManager();

        /* Enable the network settings */
        if ( state.equals( MvvmState.RUNNING ) || this.isUpgrade ) {
            logger.debug( "enabling network spaces settings because user powered on nat or upgrade." );

            try {
                networkManager.enableNetworkSpaces();
            } catch ( Exception e ) {
                throw new TransformStartException( "Unable to enable network spaces", e );
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
        } catch( TransformException e ) {
            logger.error( "Could not configure the handler.", e );
            throw new TransformStartException( "Unable to configure the handler" );
        } catch( NetworkException e ) {
            logger.error( "Could not start services.", e );
            throw new TransformStartException( "Unable to configure the handler" );
        }

        statisticManager.start();
    }

    protected void postStart()
    {
        /* Kill all active sessions */
        shutdownMatchingSessions();
    }

    protected void postStop() throws TransformStopException
    {
        /* Kill all active sessions */
        shutdownMatchingSessions();

        MvvmLocalContext context = MvvmContextFactory.context();

        MvvmState state = context.state();

        LocalNetworkManager networkManager = context.networkManager();

        /* Only stop the services if the box isn't going down (the user turned off the appliance) */
        if ( state.equals( MvvmState.RUNNING ))  {
            logger.debug( "Disabling services since user turned off network spaces." );
            networkManager.stopServices();
        }

        dhcpMonitor.stop();

        statisticManager.stop();

        /* deconfigure the event handle */
        handler.deconfigure();

        /* Deconfigure the network spaces */
        /* Only stop the services if the box isn't going down (the user turned off the appliance) */
        if ( state.equals( MvvmState.RUNNING )) {
            logger.debug( "Disabling network spaces since user turned off network spaces." );
            try {
                networkManager.disableNetworkSpaces();
            } catch ( Exception e ) {
                logger.error( "Unable to disable network spaces", e );
            }
        }
    }

    @Override protected void postDestroy() throws TransformException
    {
        /* Deregister the network settings listener */
        getNetworkManager().unregisterListener( this.listener );

        MvvmContextFactory.context().localPhoneBook().unregisterAssistant( this.assistant );
    }

    public void networkSettingsEvent( ) throws TransformException
    {
        logger.info("networkSettingsEvent");

        /* ????, what goes here. Configure the handler */

        /* Retrieve the new settings from the network manager */
        LocalNetworkManager nm = getNetworkManager();
        NetworkSpacesInternalSettings networkSettings = nm.getNetworkInternalSettings();
        ServicesInternalSettings servicesSettings = nm.getServicesInternalSettings();

        if ( getRunState() == TransformState.RUNNING ) {
            /* Have to configure DHCP before the handler, this automatically starts the dns server */
            configureDhcpMonitor( servicesSettings.getIsDhcpEnabled());
            this.handler.configure( networkSettings );
        } else {
            nm.stopServices();
            this.handler.deconfigure();
        }

        this.assistant.configure( servicesSettings );
    }


    private void updateToCurrent( NatSettings settings )
    {
        if (settings == null) {
            logger.error("NULL Nat Settings");
        } else {
            logger.info( "Update Settings Complete" );
        }
    }

    /* Kill all sessions when starting or stopping this transform */
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
        return getNatSettings();
    }

    public void setSettings(Object settings) throws Exception
    {
        setNatSettings((NatCommonSettings)settings);
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
            NatBasicSettings defaultSettings =
                this.settingsManager.getDefaultSettings( this.getTid());

            defaultSettings.setNatInternalAddress( NatUtil.SETUP_INTERNAL_ADDRESS );
            defaultSettings.setNatInternalSubnet( NatUtil.SETUP_INTERNAL_SUBNET );
            defaultSettings.setDhcpStartAndEndAddress( NatUtil.SETUP_DHCP_START, NatUtil.SETUP_DHCP_END );

            NetworkSpacesSettings newNetworkSettings = this.settingsManager.
                toNetworkSettings( networkSettings, (NatBasicSettings)defaultSettings );

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
        return MvvmContextFactory.context().networkManager();
    }
    private NetworkSpacesInternalSettings getNetworkSettings()
    {
        return getNetworkManager().getNetworkInternalSettings();
    }

    private ServicesInternalSettings getServicesSettings()
    {
        return getNetworkManager().getServicesInternalSettings();
    }

    NatSessionManager getSessionManager()
    {
        return sessionManager;
    }

    class SettingsListener
        implements NetworkSettingsListener, TransformContextSwitcher.Event<NetworkSpacesInternalSettings>
    {
        /* Use this to automatically switch context */
        private final TransformContextSwitcher<NetworkSpacesInternalSettings> tcs;

        /* This are the settings passed in by the network settings */
        private NetworkSpacesInternalSettings settings;

        SettingsListener()
        {
            tcs = new TransformContextSwitcher<NetworkSpacesInternalSettings>( getTransformContext());
        }

        public void event( NetworkSpacesInternalSettings settings )
        {
            tcs.run( this, settings );
        }

        /* TransformContextSwitcher.Event */
        public void handle( NetworkSpacesInternalSettings settings )
        {
            if ( logger.isDebugEnabled()) logger.debug( "network settings changed:" + settings );
            try {
                networkSettingsEvent();
            } catch( TransformException e ) {
                logger.error( "Unable to reconfigure the NAT transform" );
            }
        }
    }
}
