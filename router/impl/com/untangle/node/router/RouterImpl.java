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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

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
import com.untangle.uvm.networking.LocalNetworkManager;
import com.untangle.uvm.networking.NetworkException;
import com.untangle.uvm.networking.NetworkSettingsListener;
import com.untangle.uvm.networking.SetupState;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.uvm.networking.internal.ServicesInternalSettings;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeContextSwitcher;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.NodeStartException;
import com.untangle.uvm.node.NodeState;
import com.untangle.uvm.node.NodeStopException;
import com.untangle.uvm.util.DataLoader;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.util.JsonClient;
import com.untangle.uvm.util.XMLRPCUtil;
import com.untangle.uvm.node.LocalADConnector;
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
    final RouterStatisticManager statisticManager;
    private final DhcpMonitor dhcpMonitor;
    /* Done with an inner class so the GUI doesn't freak out about not
     * having the NetworkSettingsListener class */
    private final SettingsListener listener;

    /* Indicate whether or not the node is starting */

    private final SoloPipeSpec natPipeSpec;
    private final SoloPipeSpec natFtpPipeSpec;

    private final PipeSpec[] pipeSpecs;

    private final EventLogger<LogEvent> eventLogger;

    /* Indication of what should happen at startup. */
    /* WIZARD : Wizard needs to be run, initialize to the default settings.
     * UPGRADE : An upgrade has been performed, need to migrate the settings.
     * DISABLED : Do nothing at startup, just start as usual.
     */
    private enum StartupType { WIZARD, UPGRADE, DISABLED };

    /** Used to turn on network spaces if the appliances is on,
     * otherwise, network spaces are not turned on at startup. */
    private StartupType startupType  = StartupType.DISABLED;

    private final Logger logger = Logger.getLogger( RouterImpl.class );

    public RouterImpl()
    {
        this.handler          = new RouterEventHandler(this);
        this.sessionManager   = new RouterSessionManager(this);
        this.statisticManager = new RouterStatisticManager(getNodeContext());
        this.dhcpMonitor      = new DhcpMonitor( this, LocalUvmContextFactory.context());
        this.listener         = new SettingsListener();

        /* Have to figure out pipeline ordering, this should always next
         * to towards the outside */
        natPipeSpec = new SoloPipeSpec
            ("nat", this, this.handler, Fitting.OCTET_STREAM, Affinity.SERVER,
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
        logger.warn( "getRouterSettings: This method is no longer used.", new Exception());

        return new RouterSettingsImpl( getTid(), SetupState.BASIC, 
                                       RouterUtil.getInstance().getEmptyLocalMatcherList());
    }

    public void setRouterSettings( RouterCommonSettings settings )
    {
        logger.warn( "setRouterSettings: This method is no longer used.", new Exception());
    }

    /* Reinitialize the settings to basic nat */
    public void resetBasic()
    {
        logger.warn( "resetBasic: This method is no longer used.", new Exception());
    }

    /* Convert the basic settings to advanced Network Spaces */
    public void switchToAdvanced()
    {
        logger.warn( "switchToAdvanced: This method is no longer used.", new Exception());
    }

    public SetupState getSetupState()
    {
        logger.warn( "getSetupState: deprecated.", new Exception());
        
        return SetupState.BASIC;
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

        /* Disable everything */

        /* deconfigure the event handle and the dhcp manager */
        dhcpMonitor.stop();

        /* Stop the statistics manager */
        statisticManager.stop();
    }

    @Override
    protected void postInit(String[] args) throws NodeException
    {
        super.postInit( args );

        /* Register a listener, this should hang out until the node is removed dies. */
        getNetworkManager().registerListener( this.listener );

        /* Check if the settings have been upgraded yet */
        DataLoader<RouterSettingsImpl> natLoader = 
            new DataLoader<RouterSettingsImpl>( "RouterSettingsImpl", getNodeContext());

        RouterSettingsImpl settings = natLoader.loadData();

        if ( settings == null ) {
            /* Router settings are typically null, as they should come from the NetworkManager. */
            logger.info( "null router settings." );
        } else {
            /* In deprecated, mode, update and save new settings */
            SetupState state = settings.getSetupState();
            if ( state.equals( SetupState.NETWORK_SHARING )) {
                logger.warn( "Settings are in the deprecated mode, ignoring settings." );
                settings.setSetupState( SetupState.BASIC );
            } else if ( state.equals( SetupState.WIZARD )) {
                /* Enable the wizard configuration at startup */
                this.startupType = StartupType.WIZARD;
            }  else {
                logger.info( "Settings are in [" + settings.getSetupState() +"]  mode, ignoring." );
            }

            /* Just delete all of the settings, settings are only used to indicate that the 
             * router should go into the wizard */
            deleteSettings();
        }
    }

    protected void preStart() throws NodeStartException
    {
        LocalUvmContext context = LocalUvmContextFactory.context();
        UvmState state = context.state();
        LocalNetworkManager networkManager = getNetworkManager();


        switch ( this.startupType ) {
        case WIZARD:
            /* Run the commands to initialize the alpaca for the wizard */
            logger.info( "Initializing the alpaca to the wizard configuration" );
            
            /* Make a synchronous request */
            try {
                JsonClient.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, "wizard_start", null );
            } catch ( Exception e ) {
                logger.warn( "Unable to initialize the wizard", e );
            }
            break;

        case UPGRADE:
            /* Not sure what to do here? */
            logger.info( "In the upgrade state." );
            break;

        case DISABLED:
            /* No longer need to do anything at router startup */
            logger.debug( "nothing to do at startup" );
        }
        
        /* no longer at startup. */
        this.startupType = StartupType.DISABLED;

        try {
            networkSettingsEvent();
        } catch ( Exception e ) {
            logger.warn( "Error in network update.", e );
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

        dhcpMonitor.stop();

        statisticManager.stop();
    }

    @Override protected void postDestroy() throws NodeException
    {
        /* Deregister the network settings listener */
        getNetworkManager().unregisterListener( this.listener );

    }

    public void networkSettingsEvent() throws NodeException
    {
        logger.info("networkSettingsEvent");

        /* Retrieve the new settings from the network manager */
        LocalNetworkManager nm = getNetworkManager();
        ServicesInternalSettings servicesSettings = nm.getServicesInternalSettings();

        /* Default to it is disabled */
        boolean isDhcpEnabled = false;

        if ( servicesSettings == null ) {
            logger.info( "null servicesSettings, defaulting isDhcpEnabled to false." );
        } else {
            isDhcpEnabled = servicesSettings.getIsDhcpEnabled();
        }

        logger.debug( "isDhcpEnabled: " + isDhcpEnabled );

        if ( isDhcpEnabled ) dhcpMonitor.start();
        else dhcpMonitor.stop();

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

    RouterSessionManager getSessionManager()
    {
        return sessionManager;
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

    private LocalNetworkManager getNetworkManager()
    {
        return LocalUvmContextFactory.context().networkManager();
    }
    
    private void deleteSettings()
    {
        TransactionWork tw = new TransactionWork() {
                public boolean doWork( Session s )
                {
                    Query q = s.createQuery( "from RouterSettingsImpl" );
                    for ( Iterator iter = q.iterate() ; iter.hasNext() ; ) {
                        RouterSettingsImpl settings = (RouterSettingsImpl)iter.next();
                        s.delete( settings );
                    }

                    return true;
                }

                public Object getResult()
                {
                    return null;
                }
            };

        getNodeContext().runTransaction( tw );
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
