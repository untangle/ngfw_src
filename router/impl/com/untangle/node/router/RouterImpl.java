/**
 * $Id$
 */
package com.untangle.node.router;

import java.util.Map;
import java.net.InetAddress;

import com.untangle.node.token.TokenAdaptor;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.networking.NetworkConfigurationListener;
import com.untangle.uvm.networking.NetworkConfiguration;
import com.untangle.uvm.node.SessionTuple;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import org.apache.log4j.Logger;

public class RouterImpl extends NodeBase implements Router
{
    private final RouterEventHandler handler;
    private final RouterSessionManager sessionManager;
    private final DhcpMonitor dhcpMonitor;
    /* Done with an inner class so the GUI doesn't freak out about not
     * having the NetworkConfigurationListener class */
    private final SettingsListener listener;

    /* Indicate whether or not the node is starting */

    private final SoloPipeSpec routerPipeSpec;
    private final SoloPipeSpec routerFtpPipeSpec;

    private final PipeSpec[] pipeSpecs;

    private final Logger logger = Logger.getLogger( RouterImpl.class );

    public RouterImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        this.handler          = new RouterEventHandler(this);
        this.sessionManager   = new RouterSessionManager(this);
        this.dhcpMonitor      = new DhcpMonitor( this );
        this.listener         = new SettingsListener();

        /**
         * Have to figure out pipeline ordering, this should always towards the server
         */
        routerPipeSpec = new SoloPipeSpec("router", this, this.handler, Fitting.OCTET_STREAM, Affinity.SERVER, SoloPipeSpec.MAX_STRENGTH - 1);

        /* This subscription has to evaluate after NAT */
        routerFtpPipeSpec = new SoloPipeSpec("router-ftp", this, new TokenAdaptor(this, new RouterFtpFactory(this)), Fitting.FTP_TOKENS, Affinity.SERVER, 0);

        pipeSpecs = new SoloPipeSpec[] { routerPipeSpec, routerFtpPipeSpec };
    }

    // package protected methods ----------------------------------------------

    RouterEventHandler getHandler()
    {
        return handler;
    }

    ArgonConnector getRouterArgonConnector()
    {
        return routerPipeSpec.getArgonConnector();
    }

    ArgonConnector getRouterFtpPipeSpec()
    {
        return routerFtpPipeSpec.getArgonConnector();
    }

    // NodeBase methods ----------------------------------------------

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
    }

    public String lookupHostname( InetAddress address )
    {
        if (dhcpMonitor != null)
            return dhcpMonitor.lookupHostname( address );
        return null;
    }
    
    @Override
    protected void postInit()
    {
        super.postInit();

        /* Register a listener, this should hang out until the node is removed dies. */
        UvmContextFactory.context().networkManager().registerListener( this.listener );
    }

    protected void preStart() 
    {
        try {
            networkSettingsEvent();
        } catch ( Exception e ) {
            logger.warn( "Error in network update.", e );
        }
    }

    protected void postStart()
    {
        /* Kill all active sessions */
        killAllSessions();
    }

    protected void postStop() 
    {
        /* Kill all active sessions */
        killAllSessions();

        dhcpMonitor.stop();
    }

    @Override protected void postDestroy() 
    {
        /* Deregister the network settings listener */
        UvmContextFactory.context().networkManager().unregisterListener( this.listener );
    }

    public void networkSettingsEvent() 
    {
        logger.debug("networkSettingsEvent");

        /* Retrieve the new settings from the network manager */
        NetworkManager nm = UvmContextFactory.context().networkManager();
        NetworkConfiguration networkSettings = nm.getNetworkConfiguration();

        /* Default to it is disabled */
        boolean isDhcpEnabled = false;

        if ( networkSettings == null ) {
            logger.warn( "null networkSettings, defaulting isDhcpEnabled to false." );
        } else {
            isDhcpEnabled = networkSettings.getDhcpServerEnabled();
        }

        logger.debug( "isDhcpEnabled: " + isDhcpEnabled );

        if ( isDhcpEnabled ) dhcpMonitor.start();
        else dhcpMonitor.stop();

    }

    RouterSessionManager getSessionManager()
    {
        return sessionManager;
    }

    class SettingsListener implements NetworkConfigurationListener
    {
        public void event( NetworkConfiguration settings )
        {
            if ( logger.isDebugEnabled()) logger.debug( "network settings changed:" + settings );
            try {
                networkSettingsEvent();
            } catch( Exception e ) {
                logger.error( "Unable to reconfigure the NAT node" );
            }
        }
    }
} 
