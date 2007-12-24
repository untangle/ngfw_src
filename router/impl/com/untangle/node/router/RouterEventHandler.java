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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.IPSession;
import com.untangle.uvm.vnet.MPipeException;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.UDPNewSessionRequest;
import com.untangle.uvm.vnet.UDPSession;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.UDPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.UDPSessionEvent;
import org.apache.log4j.Logger;

import static com.untangle.node.router.RouterConstants.*;

class RouterEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(RouterEventHandler.class);

    private static final String PROPERTY_BASE = "com.untangle.node.router.";
    private static final String PROPERTY_TCP_PORT_START = PROPERTY_BASE + "tcp-port-start";
    private static final String PROPERTY_TCP_PORT_END   = PROPERTY_BASE + "tcp-port-end";
    private static final String PROPERTY_UDP_PORT_START = PROPERTY_BASE + "udp-port-start";
    private static final String PROPERTY_UDP_PORT_END   = PROPERTY_BASE + "udp-port-end";
    private static final String PROPERTY_ICMP_PID_START = PROPERTY_BASE + "icmp-pid-start";
    private static final String PROPERTY_ICMP_PID_END   = PROPERTY_BASE + "icmp-pid-end";

    /* tracks the open TCP ports for NAT */
    private final PortList tcpPortList;

    /* Tracks the open UDP ports for NAT */
    private final PortList udpPortList;

    /* Tracks the open ICMP identifiers, Not exactly a port, but same kind of thing */
    private final PortList icmpPidList;

    /* Router Node */
    private final RouterImpl node;

    /* reserved port for NATing TCP */
    private int reservedPort;

    /* Setup  */
    RouterEventHandler(RouterImpl node)
    {
        super(node);

        int start = Integer.getInteger(PROPERTY_TCP_PORT_START, TCP_NAT_PORT_START);
        int end = Integer.getInteger(PROPERTY_TCP_PORT_END, TCP_NAT_PORT_END);
        tcpPortList = PortList.makePortList(start, end);

        start = Integer.getInteger(PROPERTY_UDP_PORT_START, UDP_NAT_PORT_START);
        end = Integer.getInteger(PROPERTY_UDP_PORT_END, UDP_NAT_PORT_END);
        udpPortList = PortList.makePortList(start, end);

        start = Integer.getInteger(PROPERTY_ICMP_PID_START, ICMP_PID_START);
        end = Integer.getInteger(PROPERTY_ICMP_PID_END, ICMP_PID_END);
        icmpPidList = PortList.makePortList(start, end);
        this.node = node;
    }

    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event)
        throws MPipeException
    {
        handleNewSessionRequest(event.sessionRequest(), Protocol.TCP);
    }

    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event)
        throws MPipeException
    {
        handleNewSessionRequest(event.sessionRequest(), Protocol.UDP);
    }

    private void handleNewSessionRequest(IPNewSessionRequest request, Protocol protocol)
        throws MPipeException
    {
	InetAddress origClientAddr = request.clientAddr();
	InetAddress newClientAddr = request.getNatFromHost();
	InetAddress origServerAddr = request.serverAddr();
	InetAddress newServerAddr = request.getNatToHost();
	if ( logger.isDebugEnabled()) {
	    logger.debug( "pre-translation: " + request.clientAddr() + ":" + request.clientPort() +  " -> "
			  + request.serverAddr() + ":" + request.serverPort());
	}
	int origClientPort = request.clientPort();
	int newClientPort  = request.getNatFromPort();
	int origServerPort = request.serverPort();
	int newServerPort  = request.getNatToPort();
	if ( logger.isDebugEnabled()) {
	    logger.debug( "post-translation: " + request.clientAddr() + ":" + request.clientPort() +  " -> "
			  + request.serverAddr() + ":" + request.serverPort());
	}

	// if  we are a redirected session, we will already be registered with the 
	// session manager. If so it will automatically delete the iptables rule that was used to 
	// start this session
	if (node.getSessionManager().isSessionRedirect(request, protocol,node)){
	    if ( logger.isDebugEnabled()) {
		logger.debug( "Found a redirected session");
	    }
	}

	reservedPort = 0; // We only mangle the client port if we are not NATing a TCP connection

	// if the kernel changed anything then we must be NATing.
	if ( !origClientAddr.equals(newClientAddr) ||
	     !origServerAddr.equals(newServerAddr) ||
	     (origClientPort != newClientPort) ||
	     (origServerPort != newServerPort) ){

	    /* Update the session information so it matches what is in the NAT info */
	    /* Here is where we have to insert the magic, just for TCP */
	    request.clientAddr( newClientAddr );
	    if( protocol == Protocol.TCP && !origClientAddr.equals(newClientAddr)){
		// if we changed the source addr of a TCP connection,
		// we will need to allocate client port manually because the kernel will not know about 
		// ports we are non-locally bound to and may try to reuse them prematurly.
		reservedPort = getNextPort( Protocol.TCP );
		if ( logger.isDebugEnabled()) {
		    logger.debug("Mangleing client port from "+newClientPort+" to "+reservedPort);
		}
		request.clientPort( reservedPort );
	    } else {
		request.clientPort( newClientPort );
	    }
	    request.serverAddr( newServerAddr );
	    request.serverPort( newServerPort );


	    if (isFtp(request,protocol)){
		if ( logger.isDebugEnabled()) {
		    logger.debug( "Ftp Session NATed, registering with the SessionManager");
		}
		node.getSessionManager().registerSession( request, protocol,
							  origClientAddr, origClientPort,
							  origServerAddr, origServerPort );
	    }else{
		if ( logger.isDebugEnabled()) {
		    logger.debug( "non-Ftp Session NATed, not registering with the SessionManager");
		}
	    }
	}else{
	    if ( logger.isDebugEnabled()) {
		logger.debug( "Session Not NATed, so not registering with the SessionManager");
	    }
	}

    }
	

    @Override
    public void handleTCPComplete(TCPSessionEvent event)
        throws MPipeException
    {
        IPSession s = event.session();
        RouterAttachment na = (RouterAttachment)s.attachment();
        if (na != null) {
            LogEvent eventToLog = na.eventToLog();
            if (eventToLog != null) {
                node.log(eventToLog);
                na.eventToLog(null);
            }
        }
    }

    @Override
    public void handleUDPComplete(UDPSessionEvent event)
        throws MPipeException
    {
        IPSession s = event.session();
        RouterAttachment na = (RouterAttachment)s.attachment();
        if (na != null) {
            LogEvent eventToLog = na.eventToLog();
            if (eventToLog != null) {
                node.log(eventToLog);
                na.eventToLog(null);
            }
        }
    }

    public void handleTCPFinalized(TCPSessionEvent event)
        throws MPipeException
    {
        cleanupSession(Protocol.TCP, event.ipsession());
    }

    public void handleUDPFinalized(UDPSessionEvent event)
        throws MPipeException
    {
        /* XXX Special case ICMP */
        UDPSession udpsession = (UDPSession)event.ipsession();

        if (udpsession.isPing()) {
            RouterAttachment attachment = (RouterAttachment)udpsession.attachment();
            int pid = udpsession.icmpId();
            int releasePid;

            if (attachment == null) {
                logger.error("null attachment on Routerd session");
                return;
            }

            releasePid = attachment.releasePort();

            if (releasePid == 0) {
                if (logger.isDebugEnabled()) logger.debug("Ignoring non-natted PID: " + pid);
            } else if (pid != releasePid) {
                /* This is now an error, because the releasePid should be zero if it is not
                 * to be released */
                logger.error("Mismatch on the attached PID and the session PID " +
                              pid + "!=" + releasePid);
            } else {
                if (logger.isDebugEnabled()) logger.debug("ICMP: Releasing pid: " + releasePid);

                icmpPidList.releasePort(releasePid);
            }
        } else {
            cleanupSession(Protocol.UDP, udpsession);
        }
    }

    /**
     * Retrieve the next port from the port list
     */
    int getNextPort(Protocol protocol)
    {
        return getPortList(protocol).getNextPort();
    }

    /**
     * Release a port
     * Utility function for RouterSessionManager.
     */
    void releasePort(Protocol protocol, int port)
    {
        getPortList(protocol).releasePort(port);
    }

    /**
     * Cleanup any of the information associated with a UDP or TCP session.
     * Presently not implemented to handle ICMP sessions.
     */
    private void cleanupSession(Protocol protocol, IPSession session)
    {
        RouterAttachment attachment = (RouterAttachment)session.attachment();

	
	node.getSessionManager().releaseSession(session, protocol);
	
	if (reservedPort != 0){
	    if (logger.isDebugEnabled()) {
		logger.debug("Releasing client port: " + reservedPort);
	    }
	    releasePort(protocol, reservedPort);
	}
	
        if (attachment == null) {
            logger.error("null attachment on Routerd session");
            return;
        }

        int releasePort = attachment.releasePort();

        if (releasePort != 0) {
            if (releasePort != session.clientPort() &&
                 releasePort != session.serverPort()) {
                /* This happens for all NAT ftp PORT sessions */
                logger.info("Release port " + releasePort +" is neither client nor server port");
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Releasing port: " + releasePort);
            }

            getPortList(protocol).releasePort(releasePort);
        } else {
            if (logger.isDebugEnabled())
                if (logger.isDebugEnabled()) {
                    logger.debug("Ignoring non-natted port: "
                                  + session.clientPort()
                                  + "/" + session.serverPort());
                }
        }

        if (attachment.isManagedSession()) {
            logger.debug("Removing session from the managed list");

            node.getSessionManager().releaseSession(session, protocol);
        }
    }

    private PortList getPortList(Protocol protocol)
    {
        if (protocol == Protocol.UDP) {
            return udpPortList;
        } else if (protocol == Protocol.TCP) {
            return tcpPortList;
        }

        throw new IllegalArgumentException("Unknown protocol: " + protocol);
    }

    private boolean isFtp(IPNewSessionRequest request, Protocol protocol)
    {
        if ((protocol == Protocol.TCP) && (request.serverPort() == FTP_SERVER_PORT)) {
            return true;
        }

        return false;
    }
}
