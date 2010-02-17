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

package com.untangle.uvm.networking;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.Netcap;
import com.untangle.jnetcap.PortRange;
import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.localapi.ArgonInterface;
import com.untangle.uvm.localapi.LocalIntfManager;
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

    /* ---------------------- PACKAGE ---------------------- */

    /* Call the script to generate all of the iptables rules */
    synchronized void generateIptablesRules() throws NetworkException
    {
        if ( isShutdown ) {
            logger.warn( "UVM is already shutting down, no longer able to generate rules" );
            return;
        }

        /* Make an asynchronous request */
        LocalUvmContextFactory.context().newThread( new GenerateRules( null )).start();
    }

    synchronized void destroyIptablesRules() throws NetworkException
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
        
        scriptWriter.appendVariable( TCP_REDIRECT_PORT_FLAG, tcp.low() + "-" + tcp.high());
        
        LocalIntfManager lim = LocalUvmContextFactory.context().localIntfManager();
        
        /* Setup all of the values for the interfaces */
        /* XXX When we want to use custom interfaces we should just redefine INTERFACE_ORDER */
        for ( ArgonInterface intf : lim.getIntfList()) {
            if ( intf.hasSecondaryName()) {
                String argonName = IntfConstants.toName( intf.getArgon()).toUpperCase();
                scriptWriter.appendVariable( "UVM_" + argonName + "_INTF", intf.getSecondaryName());
            }
        }
        
        /* Add the flag to redirect traffic from 443, to the special internal open port */
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
            try {
                JsonClient.getInstance().callAlpaca( XMLRPCUtil.CONTROLLER_UVM, "generate_rules", null );
            } catch ( Exception e ) {
                logger.error( "Error while generating iptables rules", e );
                this.exception = e;
            }

            if ( this.callback != null ) this.callback.run();
        }
        
        public Exception getException()
        {
            return this.exception;
        }
    }
}
