/**
 * $Id$
 */
package com.untangle.node.router;

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.node.token.TokenAdaptor;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.SoloPipeSpec;

public class RouterImpl extends NodeBase implements Router
{
    private final RouterEventHandler handler;
    private final RouterSessionManager sessionManager;
    private final DhcpMonitor dhcpMonitor;

    private final SoloPipeSpec routerPipeSpec;
    private final SoloPipeSpec routerFtpPipeSpecCtl;
    private final SoloPipeSpec routerFtpPipeSpecData;

    private final PipeSpec[] pipeSpecs;

    private final Logger logger = Logger.getLogger( RouterImpl.class );

    public RouterImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        this.handler          = new RouterEventHandler(this);
        this.sessionManager   = new RouterSessionManager(this);
        this.dhcpMonitor      = new DhcpMonitor( this );

        /**
         * Have to figure out pipeline ordering, this should always towards the server
         */
        routerPipeSpec = new SoloPipeSpec("router", this, this.handler, Fitting.OCTET_STREAM, Affinity.SERVER, SoloPipeSpec.MAX_STRENGTH - 1);

        /**
         * This subscription has to evaluate after NAT
         */
        //routerFtpPipeSpec = new SoloPipeSpec("router-ftp", this, new TokenAdaptor(this, new RouterFtpFactory(this)), Fitting.FTP_TOKENS, Affinity.SERVER, 0);
        routerFtpPipeSpecCtl = new SoloPipeSpec("router-ftp-ctl", this, new TokenAdaptor(this, new RouterFtpFactory(this)), Fitting.FTP_CTL_TOKENS, Affinity.SERVER, 0);
   		routerFtpPipeSpecData = new SoloPipeSpec("router-ftp-data", this, new TokenAdaptor(this, new RouterFtpFactory(this)), Fitting.FTP_DATA_TOKENS, Affinity.SERVER, 0);

        pipeSpecs = new SoloPipeSpec[] { routerPipeSpec, routerFtpPipeSpecCtl, routerFtpPipeSpecData };
    }

    // package protected methods ----------------------------------------------

    RouterEventHandler getHandler()
    {
        return handler;
    }

    PipelineConnector getRouterPipelineConnector()
    {
        return routerPipeSpec.getPipelineConnector();
    }

    PipelineConnector getRouterFtpPipeSpecCtl()
    {
        return routerFtpPipeSpecCtl.getPipelineConnector();
    }
    
    PipelineConnector getRouterFtpPipeSpecData()
    {
        return routerFtpPipeSpecData.getPipelineConnector();
    }

    // NodeBase methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    public String lookupHostname( InetAddress address )
    {
        if (dhcpMonitor != null)
            return dhcpMonitor.lookupHostname( address );
        return null;
    }
    
    protected void preStart() 
    {
        dhcpMonitor.start();
    }

    protected void postStop() 
    {
        killAllSessions();

        dhcpMonitor.stop();
    }

    RouterSessionManager getSessionManager()
    {
        return sessionManager;
    }
} 
