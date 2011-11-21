/**
 * $Id$
 */
package com.untangle.uvm.networking;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.Netcap;
import com.untangle.jnetcap.PortRange;
import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.localapi.ArgonInterface;
import com.untangle.uvm.node.script.ScriptWriter;
import com.untangle.uvm.util.JsonClient;
import com.untangle.uvm.util.XMLRPCUtil;

public class RuleManager
{
    private static final String TCP_REDIRECT_PORT_FLAG       = "TCP_REDIRECT_PORTS";
    /* Flags to set the redirect for traffic to the internal admin port */
    private static final String INTERNAL_OPEN_REDIRECT_FLAG  = "HTTPS_INTERNAL_REDIRECT_PORT";

    /* This is the default port range, rarely do these ever vary */
    private static final PortRange DEFAULT_TCP_PORT_RANGE = new PortRange( 9500, 9627 );

    private static RuleManager INSTANCE = null;

    private final Logger logger = Logger.getLogger( getClass());

    private boolean isShutdown = false;

    private static final Object lock = new Object();
    
    /* ---------------------- PACKAGE ---------------------- */

    /* Call the script to generate all of the iptables rules */
    synchronized void generateIptablesRules() throws Exception
    {
        if ( isShutdown ) {
            logger.warn( "UVM is already shutting down, no longer able to generate rules" );
            return;
        }

        /* Make an asynchronous request */
        LocalUvmContextFactory.context().newThread( new GenerateRules( null )).start();
    }

    synchronized void destroyIptablesRules() throws Exception
    {
        if ( isShutdown ) {
            logger.warn( "UVM is already shutting down, no longer able to generate rules" );
            return;
        }

        /* Make an asynchronous request */
        LocalUvmContextFactory.context().newThread( new GenerateRules( null )).start();
    }

    synchronized void isShutdown()
    {
        this.isShutdown = true;
    }

    void commit( ScriptWriter scriptWriter )
    {
        Netcap netcap = Netcap.getInstance();
            
        PortRange tcp = DEFAULT_TCP_PORT_RANGE;

        try {
            tcp = netcap.tcpRedirectPortRange();
        } catch ( Exception e ) {
            logger.error( "unable to determine the TCP redirect ports, using default", e );
            tcp = DEFAULT_TCP_PORT_RANGE;
        }
        
        scriptWriter.appendLine("# Ports that the Untangle-vm is listening for incoming TCP connections");
        scriptWriter.appendVariable( TCP_REDIRECT_PORT_FLAG, tcp.low() + "-" + tcp.high());
        
        /* Add the flag to redirect traffic from 443, to the special internal open port */
        scriptWriter.appendLine("# The local HTTPS port");
        scriptWriter.appendVariable( INTERNAL_OPEN_REDIRECT_FLAG, NetworkUtil.INTERNAL_OPEN_HTTPS_PORT );
    }

    static synchronized RuleManager getInstance()
    {
        if ( INSTANCE == null ) {
            INSTANCE = new RuleManager();
        }
        
        return INSTANCE;
    }

    class GenerateRules implements Runnable
    {
        private Exception exception;
        private final Runnable callback;
        
        public GenerateRules( Runnable callback )
        {
            this.callback = callback;
        }

        public void run()
        {
            int tryCount = 0;
            boolean success = false;

            synchronized(RuleManager.lock) {
                do {
                    try {
                        JsonClient.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, "generate_rules", null );
                        success = true;
                        break;
                    } catch ( Exception e ) {
                        logger.warn( "Error while generating iptables rules (trying again...)", e );
                        this.exception = e;
                    }

                    try {Thread.sleep(3000);} catch(Exception e) {}
                    tryCount++;
                }
                while (tryCount < 5);
            }
            
            if (!success) {
                logger.error( "Failed to generate iptables rules.");
            }

            if ( this.callback != null ) this.callback.run();
        }
        
        public Exception getException()
        {
            return this.exception;
        }
    }
}
