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

import com.untangle.node.token.TokenAdaptor;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.localapi.SessionMatcher;
import com.untangle.uvm.localapi.SessionMatcherFactory;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.networking.LocalNetworkManager;
import com.untangle.uvm.networking.NetworkSettingsListener;
import com.untangle.uvm.networking.SetupState;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.uvm.networking.internal.ServicesInternalSettings;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.NodeStartException;
import com.untangle.uvm.node.NodeStopException;
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
        natPipeSpec = new SoloPipeSpec("nat", this, this.handler, Fitting.OCTET_STREAM, Affinity.SERVER, SoloPipeSpec.MAX_STRENGTH - 1);

        /* This subscription has to evaluate after NAT */
        natFtpPipeSpec = new SoloPipeSpec("nat-ftp", this, new TokenAdaptor(this, new RouterFtpFactory(this)), Fitting.FTP_TOKENS, Affinity.SERVER, 0);

        pipeSpecs = new SoloPipeSpec[] { natPipeSpec, natFtpPipeSpec };

        NodeContext tctx = getNodeContext();
        eventLogger = EventLoggerFactory.factory().getEventLogger(tctx);
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
        LocalUvmContextFactory.context().localNetworkManager().registerListener( this.listener );
    }

    protected void preStart() throws NodeStartException
    {
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
        killMatchingSessions(SessionMatcherFactory.getAllInstance());
    }

    protected void postStop() throws NodeStopException
    {
        /* Kill all active sessions */
        killMatchingSessions(SessionMatcherFactory.getAllInstance());

        dhcpMonitor.stop();

        statisticManager.stop();
    }

    @Override protected void postDestroy() throws NodeException
    {
        /* Deregister the network settings listener */
        LocalUvmContextFactory.context().localNetworkManager().unregisterListener( this.listener );
    }

    public void networkSettingsEvent() throws NodeException
    {
        logger.info("networkSettingsEvent");

        /* Retrieve the new settings from the network manager */
        LocalNetworkManager nm = LocalUvmContextFactory.context().localNetworkManager();
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

    RouterSessionManager getSessionManager()
    {
        return sessionManager;
    }

    void log(LogEvent le)
    {
        eventLogger.log(le);
    }

    class SettingsListener implements NetworkSettingsListener
    {
        public void event( NetworkSpacesInternalSettings settings )
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
