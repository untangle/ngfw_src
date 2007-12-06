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

import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.Netcap;
import com.untangle.jnetcap.JNetcapException;
import com.untangle.jnetcap.PortRange;

import com.untangle.uvm.ArgonException;
import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.localapi.ArgonInterface;
import com.untangle.uvm.localapi.LocalIntfManager;

import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.script.ScriptRunner;
import com.untangle.uvm.node.script.ScriptWriter;

import com.untangle.uvm.networking.internal.InterfaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpaceInternal;

import static com.untangle.uvm.networking.NetworkManagerImpl.BUNNICULA_BASE;
import static com.untangle.uvm.networking.NetworkManagerImpl.BUNNICULA_CONF;

public class RuleManager
{
    private static final String UDP_DIVERT_PORT_FLAG         = "UDP_DIVERT_PORT";
    private static final String TCP_REDIRECT_PORT_FLAG       = "TCP_REDIRECT_PORTS";
    private static final String ANTISUBSCRIBE_LOCAL_IN_FLAG  = "ANTISUBSCRIBE_LOCAL_INSIDE";
    private static final String ANTISUBSCRIBE_LOCAL_OUT_FLAG = "ANTISUBSCRIBE_LOCAL_OUTSIDE";
    private static final String DHCP_BLOCK_FORWARD_FLAG      = "DHCP_BLOCK_FORWARDING";
    /* Flags to set the redirect for traffic to the internal admin port */
    private static final String INTERNAL_OPEN_REDIRECT_FLAG  = "HTTPS_INTERNAL_REDIRECT_PORT";

    /* Flags to use to steal an address and a few ports */
    private static final String SETUP_MODE_FLAG              = "IS_IN_SETUP_MODE";
    private static final String SETUP_ADDRESS_FLAG           = "STEAL_ADDRESS";
    private static final String SETUP_INTERFACE_FLAG         = "STEAL_INTERFACE";
    private static final String SETUP_TCP_PORTS_FLAG         = "STEAL_TCP_PORTS";
    private static final String SETUP_UDP_PORTS_FLAG         = "STEAL_UDP_PORTS";

    /* Set to a list of interfaces that are in the services space that need to be able to
     * access the services */
    private static final String SERVICES_INTERFACE_LIST      = "UVM_SERVICES_INTF_LIST";

    /* Set to the index of the interfaces where ping should be enabled. */
    private static final String PING_ANTISUBSCRIBE_FLAG      = "UVM_PING_EN";
    private static final String PING_ANTISUBSCRIBE_LIST      = "UVM_PING_LIST";

    /* This is the default port range, rarely do these ever vary */
    private static final PortRange DEFAULT_TCP_PORT_RANGE = new PortRange( 9500, 9627 );
    private static final int DEFAULT_DIVERT_PORT = 9500;

    private static RuleManager INSTANCE = null;

    private final String UVM_TMP_FILE  = BUNNICULA_CONF + "/tmp_params";

    private final String RULE_GENERATOR_SCRIPT = BUNNICULA_BASE + "/networking/rule-generator";
    private final String RULE_DESTROYER_SCRIPT = BUNNICULA_BASE + "/networking/rule-destroyer";

    private final Logger logger = Logger.getLogger( getClass());

    private boolean subscribeLocalInside  = true;
    private boolean subscribeLocalOutside = true;
    private boolean dhcpEnableForwarding  = true;

    private boolean isShutdown = false;

    /* Set to true in order to use the value specified in the interface list */
    private boolean pingInterfaceEnable = false;

    /* List of the interfaces where ping is enabled */
    private String pingInterfaceList = "";

    /* List of interfaces that are in the services spaces */
    private String servicesInterfaceList = "";

    /* True if setup has been completed */
    private boolean hasCompletedSetup = true;

    /* ---------------------- PACKAGE ---------------------- */

    /* Call the script to generate all of the iptables rules */
    synchronized void generateIptablesRules() throws NetworkException
    {
        if ( isShutdown ) {
            logger.warn( "UVM is already shutting down, no longer able to generate rules" );
            return;
        }

        try {
            ScriptRunner.getInstance().exec( RULE_GENERATOR_SCRIPT );
        } catch ( Exception e ) {
            logger.error( "Error while generating iptables rules", e );
            throw new NetworkException( "Unable to generate iptables rules", e );
        }        
    }

    synchronized void destroyIptablesRules() throws NetworkException
    {
        try {
            /* Call the rule generator */
            ScriptRunner.getInstance().exec( RULE_DESTROYER_SCRIPT );            
        } catch ( Exception e ) {
            logger.error( "Error while removing iptables rules", e );
            throw new NetworkException( "Unable to remove iptables rules", e );
        }        
    }

    void subscribeLocalInside( boolean subscribeLocalInside )
    {
        this.subscribeLocalInside = subscribeLocalInside;
    }

    void subscribeLocalOutside( boolean subscribeLocalOutside )
    {
        this.subscribeLocalOutside = subscribeLocalOutside;
    }
    
    void dhcpEnableForwarding( boolean dhcpEnableForwarding )
    {
        this.dhcpEnableForwarding = dhcpEnableForwarding;
    }

    /* Just used to setup the antisubscribes */
    void setInterfaceList( List<InterfaceInternal> interfaceList, NetworkSpaceInternal serviceSpace )
    {
        String pingAntisubscribeList = "";
        
        String servicesInterfaceList = "";
        
        LocalIntfManager lim = LocalUvmContextFactory.context().localIntfManager();

        for ( InterfaceInternal intf : interfaceList ) {
            ArgonInterface argonIntf = intf.getArgonIntf();
            if ( serviceSpace != null && serviceSpace.equals( intf.getNetworkSpace())) {
                switch( argonIntf.getArgon()) {
                case IntfConstants.EXTERNAL_INTF:
                    /* Always ignore the external interface */
                    break;
                    
                case IntfConstants.DMZ_INTF:
                    /* DMZ interface is not added if it is in the public space */
                    if ( 0 == serviceSpace.getIndex()) break;
                    
                    /* fallthrough */
                default:
                    /* All other interfaces are always added unconditionally */
                    servicesInterfaceList += " " + argonIntf.getName();
                }
            }
            
            if ( intf.isPingable()) {
                pingAntisubscribeList = pingAntisubscribeList + " " + argonIntf.getNetcap();
            }
        }
        
        this.pingInterfaceEnable = true;
        this.pingInterfaceList   = pingAntisubscribeList.trim();
        this.servicesInterfaceList = servicesInterfaceList.trim();
    }

    void setHasCompletedSetup( boolean newValue )
    {
        this.hasCompletedSetup = newValue;
    }

    synchronized void isShutdown()
    {
        this.isShutdown = true;
    }

    void commit( ScriptWriter scriptWriter )
    {
        Netcap netcap = Netcap.getInstance();
            
        PortRange tcp = DEFAULT_TCP_PORT_RANGE;
        int divertPort = DEFAULT_DIVERT_PORT;

        try {
            tcp = netcap.tcpRedirectPortRange();
            divertPort = netcap.udpDivertPort();
        } catch ( JNetcapException e ) {
            logger.error( "unable to determine the TCP or UDP redirect ports, using default", e );
            tcp = DEFAULT_TCP_PORT_RANGE;
            divertPort = DEFAULT_DIVERT_PORT;
        }
        
        scriptWriter.appendVariable( TCP_REDIRECT_PORT_FLAG, tcp.low() + "-" + tcp.high());
        scriptWriter.appendVariable( UDP_DIVERT_PORT_FLAG, divertPort );
        scriptWriter.appendVariable( ANTISUBSCRIBE_LOCAL_IN_FLAG, !subscribeLocalInside );
        scriptWriter.appendVariable( ANTISUBSCRIBE_LOCAL_OUT_FLAG, !subscribeLocalOutside );
        scriptWriter.appendVariable( DHCP_BLOCK_FORWARD_FLAG, !dhcpEnableForwarding );

        if ( pingInterfaceEnable ) {
            scriptWriter.appendVariable( PING_ANTISUBSCRIBE_FLAG, true );
            scriptWriter.appendVariable( PING_ANTISUBSCRIBE_LIST, this.pingInterfaceList );
        }
        
        if (( null != this.servicesInterfaceList ) && ( this.servicesInterfaceList.length() > 0 )) {
            scriptWriter.appendVariable( SERVICES_INTERFACE_LIST, this.servicesInterfaceList );
        }
        
        LocalIntfManager lim = LocalUvmContextFactory.context().localIntfManager();
        
        /* Setup a rule for stealing ARPs */
        if ( !this.hasCompletedSetup ) {
            String internal = "eth1";
            
            internal = lim.getInternal().getName();
            
            scriptWriter.appendVariable( SETUP_MODE_FLAG, true );
            scriptWriter.appendVariable( SETUP_ADDRESS_FLAG, "" + NetworkUtil.SETUP_ADDRESS );
            scriptWriter.appendVariable( SETUP_INTERFACE_FLAG, internal );
            /* Steal ports 80 and 443 for HTTP and HTTPs */
            scriptWriter.appendVariable( SETUP_TCP_PORTS_FLAG, "80,443" );
            
            /* Steal port 53 for DHCP */
            scriptWriter.appendVariable( SETUP_UDP_PORTS_FLAG, "53" );
        }
        
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
}
