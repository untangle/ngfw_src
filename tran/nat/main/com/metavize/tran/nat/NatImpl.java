/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.nat;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.MvvmLocalContext;

import com.metavize.mvvm.networking.NetworkManagerImpl;
import com.metavize.mvvm.networking.NetworkException;
import com.metavize.mvvm.networking.RedirectRule;
import com.metavize.mvvm.networking.SetupState;
import com.metavize.mvvm.networking.NetworkSpacesSettings;
import com.metavize.mvvm.networking.ServicesSettings;
import com.metavize.mvvm.networking.NetworkSettingsListener;
import com.metavize.mvvm.networking.internal.NetworkSpacesInternalSettings;
import com.metavize.mvvm.networking.internal.ServicesInternalSettings;
import com.metavize.mvvm.argon.SessionMatcher;
import com.metavize.mvvm.argon.SessionMatcherFactory;
import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.logging.EventLoggerFactory;
import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.SimpleEventFilter;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.tran.TransformState;
import com.metavize.mvvm.tran.TransformStopException;

import com.metavize.tran.token.TokenAdaptor;

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

    private final SoloPipeSpec natPipeSpec;
    private final SoloPipeSpec natFtpPipeSpec;

    private final PipeSpec[] pipeSpecs;

    private final EventLogger<LogEvent> eventLogger;

    private final Logger logger = Logger.getLogger( NatImpl.class );

    public NatImpl()
    {
        this.handler          = new NatEventHandler(this);
        this.sessionManager   = new NatSessionManager(this);
        this.statisticManager = new NatStatisticManager(getTransformContext());
        this.settingsManager  = new SettingsManager();
        this.dhcpMonitor      = new DhcpMonitor( this, MvvmContextFactory.context());
        this.listener         = new SettingsListener();

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
        NetworkManagerImpl nm = getNetworkManager();
        
        SetupState state = getSetupState();

        NetworkSpacesSettings network = nm.getNetworkSettings();
        NetworkSpacesInternalSettings networkInternal = nm.getNetworkInternalSettings();
        ServicesInternalSettings servicesInternal = nm.getServicesInternalSettings();
        
        if ( state.equals( SetupState.BASIC )) {
            return settingsManager.toBasicSettings( this.getTid(), networkInternal, servicesInternal );
        } else if ( state.equals( SetupState.ADVANCED )) {
            return settingsManager.toAdvancedSettings( network, servicesInternal );
        }
        
        logger.error( "Invalid state: [" + state + "] using basic" );

        return settingsManager.toBasicSettings( this.getTid(), networkInternal, servicesInternal );
    }
        
    public void setNatSettings( NatCommonSettings settings ) throws Exception
    {        
        /* Remove all of the non-static addresses before saving */
        // !!!! Pushed into the networking package
        // dhcpManager.fleeceLeases( settings );
        
        /* Validate the settings */
        try {
            settings.validate();
        }
        catch ( Exception e ) {
            logger.error("Invalid NAT settings", e);
            throw e;
        }

        NetworkManagerImpl networkManager = getNetworkManager();
        
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

        /* This isn't necessary, (the state should carry over), but just in case. */
        newNetworkSettings.setIsEnabled( getRunState() == TransformState.RUNNING );
        
        try {
            /* Have to reconfigure the network before configure the services settings */
            networkManager.setNetworkSettings( newNetworkSettings );
            networkManager.setServicesSettings( settings );
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
        NetworkManagerImpl nm = getNetworkManager();
        
        NetworkSpacesSettings newSettings = 
            this.settingsManager.resetToBasic( getTid(), nm.getNetworkSettings());
        
        nm.setNetworkSettings( newSettings );
    }
    
    /* Convert the basic settings to advanced Network Spaces */
    public void switchToAdvanced() throws Exception
    {
        /* Get the settings from Network Spaces (The only state in the transform is the setup state) */
        NetworkManagerImpl nm = getNetworkManager();
        
        NetworkSpacesSettings newSettings = this.settingsManager.basicToAdvanced( nm.getNetworkSettings());
        
        nm.setNetworkSettings( newSettings );
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

    protected void initializeSettings()
    {
        logger.info("Initializing Settings...");

        NatBasicSettings settings = settingsManager.getDefaultSettings( this.getTid());

        /* Disable everything */

        /* deconfigure the event handle and the dhcp manager */
        // !!!! Pushed into the networking package
        // dhcpManager.deconfigure();
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
    protected void postInit(String[] args)
    {
        /* Register a listener, this should hang out until the transform is removed dies. */
        getNetworkManager().registerListener( this.listener );
    }

    protected void preStart() throws TransformStartException
    {
        eventLogger.start();

        MvvmLocalContext context = MvvmContextFactory.context();
        MvvmLocalContext.MvvmState state = context.state();
        NetworkManagerImpl networkManager = getNetworkManager();
        
        NetworkSpacesInternalSettings networkSettings = getNetworkSettings();
        ServicesInternalSettings servicesSettings = getServicesSettings();

        /* Have to configure DHCP before the handler */
        // ??????
//         try {
//             if ( !state.equals( MvvmLocalContext.MvvmState.LOADED ))  {
//                 logger.debug( "Enabling services since user turned on network spaces." );
                
//             }
//         } catch ( NetworkException e ) {
//             logger.error( "Unable to configure DHCP/DNS server", e );
//             throw new TransformStartException( "Unable to configure DHCP/DNS server" );
//         }
        
        try {
            configureDhcpMonitor( servicesSettings.getIsDhcpEnabled());
            this.handler.configure( networkSettings );

            // !!!! Pushed into the networking package dhcpManager.startDnsMasq();
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
        
        MvvmLocalContext.MvvmState state = context.state();

        NetworkManagerImpl networkManager = (NetworkManagerImpl)context.networkManager();
        
        /* Only stop the services if the box isn't going down (the user turned off the appliance) */
        if ( !state.equals( MvvmLocalContext.MvvmState.DESTROYED ))  {
            logger.debug( "Disabling services since user turned off network spaces." );
            networkManager.stopServices();
        }
        
        dhcpMonitor.stop();

        statisticManager.stop();

        /* deconfigure the event handle */
        handler.deconfigure();

        /* Deconfigure the network spaces */
        /* Only stop the services if the box isn't going down (the user turned off the appliance) */
        if ( !state.equals( MvvmLocalContext.MvvmState.DESTROYED )) {
            logger.debug( "Disabling network spaces since user turned off network spaces." );
            networkManager.disableNetworkSpaces();
        }

        eventLogger.stop();
    }

    @Override protected void postDestroy() throws TransformException
    {
        /* Register a listener, this should hang out until the transform is removed dies. */
        getNetworkManager().unregisterListener( this.listener );
    }

    public void reconfigure() throws TransformException
    {
        logger.info("Reconfigure()");

        /* ????, what goes here. Configure the handler */
        
        /* Retrieve the new settings from the network manager */
        NetworkManagerImpl networkManager = (NetworkManagerImpl)MvvmContextFactory.context().networkManager();
        NetworkSpacesInternalSettings networkSettings = networkManager.getNetworkInternalSettings();
        ServicesInternalSettings servicesSettings = getServicesSettings();
        
        if ( getRunState() == TransformState.RUNNING ) {
            /* Have to configure DHCP before the handler, this automatically starts the dns server */
            configureDhcpMonitor( servicesSettings.getIsDhcpEnabled());
            this.handler.configure( networkSettings );
        } else {
            networkManager.stopServices();
        }
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
    
    private NetworkManagerImpl getNetworkManager()
    {
        return (NetworkManagerImpl)MvvmContextFactory.context().networkManager();
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

    class SettingsListener implements NetworkSettingsListener
    {
        public void event( NetworkSpacesInternalSettings settings )
        {
            logger.debug( "Listener hath been called" );
        }
        
    }
}
