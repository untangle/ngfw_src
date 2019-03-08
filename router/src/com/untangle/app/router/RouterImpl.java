/**
 * $Id$
 */
package com.untangle.app.router;

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.app.HostnameLookup;

/**
 * RouterImpl is the router app
 */
public class RouterImpl extends AppBase implements HostnameLookup
{
    private final RouterEventHandler handler;
    private final DhcpMonitor dhcpMonitor;

    private final PipelineConnector routerConnector;

    private final PipelineConnector[] connectors;

    private final Logger logger = Logger.getLogger( RouterImpl.class );

    /**
     * RouterImpl
     * @param appSettings
     * @param appProperties
     */
    public RouterImpl( com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties )
    {
        super( appSettings, appProperties );

        this.handler          = new RouterEventHandler(this);
        this.dhcpMonitor      = new DhcpMonitor( this );

        /**
         * Have to figure out pipeline ordering, this should always towards the server
         */
        routerConnector = UvmContextFactory.context().pipelineFoundry().create("router", this, null, this.handler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.SERVER, 32 - 1, false );

        connectors = new PipelineConnector[] { routerConnector };
    }

    /**
     * getConnectors - get the pipeline connectors
     * @return PipelineConnector[]
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * lookupHostname - lookup the hostname in DHCP
     * @param address - the address to lookup
     * @return the hostname or null
     */
    public String lookupHostname( InetAddress address )
    {
        if (dhcpMonitor != null)
            return dhcpMonitor.lookupHostname( address );
        return null;
    }
    
    /**
     * preStart
     * @param isPermanentTransition
     */
    @Override
    protected void preStart( boolean isPermanentTransition ) 
    {
        dhcpMonitor.start();
    }

    /**
     * postStop
     * @param isPermanentTransition
     */
    @Override
    protected void postStop( boolean isPermanentTransition ) 
    {
        killAllSessions();

        dhcpMonitor.stop();
    }
} 
