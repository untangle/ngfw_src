package com.untangle.uvm;

import java.net.URI;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Random;

import org.apache.log4j.Logger;

import com.untangle.uvm.Plugin;
import com.untangle.uvm.HostTableEntry;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.node.http.HttpEventHandler;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.StatusLine;
import com.untangle.node.http.HeaderToken;

public class ExamplePluginImpl implements Plugin
{
    private static final Logger logger = Logger.getLogger( ExamplePluginImpl.class );

    protected PipelineConnector connector = null;

    private ExamplePluginImpl() {}
    
    public static Plugin instance()
    {
        return new ExamplePluginImpl();
    }
    
    public final void run()
    {
        logger.info("ExamplePlugin run()");
        
        Node httpCasing = UvmContextFactory.context().nodeManager().node("untangle-casing-http");
        if ( httpCasing == null ) {
            logger.warn("HTTP Casing not loaded. ExamplePlugin not loaded.");
            return;
        }
        if ( httpCasing.getRunState() != NodeSettings.NodeState.RUNNING ) {
            logger.warn("HTTP Casing not running (" + httpCasing.getRunState() + "). ExamplePlugin not loaded.");
            return;
        }
        if ( !isAppRunning("web-filter") &&
             !isAppRunning("web-filter-lite") &&
             !isAppRunning("virus-blocker-lite") &&
             !isAppRunning("virus-blocker") ) {
            logger.warn("No common apps running. ExamplePlugin not loaded.");
            return;
        }

        Node shield = UvmContextFactory.context().nodeManager().node("untangle-node-shield");
        if ( shield == null || shield.getRunState() != NodeSettings.NodeState.RUNNING ) {
            logger.warn("Shield not running. ExamplePlugin not loaded.");
            return;
        }
        
        try { Thread.sleep(1000); } catch (Exception e) {}

        try {
            logger.info("Loading extension...");
            this.connector = UvmContextFactory.context().pipelineFoundry().create("extension", shield, null, new ExamplePluginEventHandler(), Fitting.HTTP_TOKENS, Fitting.HTTP_TOKENS, Affinity.CLIENT, 1, false);
            UvmContextFactory.context().pipelineFoundry().registerPipelineConnector( connector );

        } catch (Exception e) {
            logger.debug("Exception",e);
        }
        logger.info("ExamplePlugin loaded.");
    }

    public final void stop()
    {
        logger.info("ExamplePlugin stop()");

        if ( this.connector != null ) {
            UvmContextFactory.context().pipelineFoundry().deregisterPipelineConnector( this.connector );
            this.connector.destroy();
            this.connector = null;
        }
    }
    
    public boolean isAppRunning( String name )
    {
        Node node = UvmContextFactory.context().nodeManager().node("untangle-node-" + name);
        if ( node == null )
            return false;
        if ( node.getRunState() == NodeSettings.NodeState.RUNNING )
            return true;
        return false;
    }

    private class ExamplePluginEventHandler extends HttpEventHandler
    {
        private void processRequest( NodeTCPSession sess, HeaderToken requestHeader )
        {
            String uri = getRequestLine( sess ).getRequestUri().toString();
            String referer = requestHeader.getValue("Referer");
            String host = requestHeader.getValue("host");
            String userAgent = requestHeader.getValue("user-agent");
            InetAddress client = sess.getOrigClientAddr();
            if ( client == null )
                return;

            logger.warn( client.getHostAddress() + " visited " + host + uri );
        }

        protected ExamplePluginEventHandler() {}

        @Override
        protected RequestLineToken doRequestLine( NodeTCPSession session, RequestLineToken requestLine )
        {
            return requestLine;
        }

        @Override
        protected HeaderToken doRequestHeader( NodeTCPSession sess, HeaderToken requestHeader )
        {
            try {
                processRequest( sess, requestHeader );
            } catch (Exception e) {
                logger.warn("Exception:",e);
            }
        
            releaseRequest( sess );
            return requestHeader;
        }

        @Override
        protected ChunkToken doRequestBody( NodeTCPSession session, ChunkToken chunk )
        {
            return chunk;
        }

        @Override
        protected void doRequestBodyEnd( NodeTCPSession session ) { }

        @Override
        protected StatusLine doStatusLine( NodeTCPSession session, StatusLine statusLine )
        {
            return statusLine;
        }

        @Override
        protected HeaderToken doResponseHeader( NodeTCPSession sess, HeaderToken responseHeader )
        {
            releaseResponse( sess );
            return responseHeader;
        }
        
        @Override
        protected ChunkToken doResponseBody( NodeTCPSession session, ChunkToken chunk )
        {
            return chunk;
        }

        @Override
        protected void doResponseBodyEnd( NodeTCPSession session ) { }
        
    }
}

