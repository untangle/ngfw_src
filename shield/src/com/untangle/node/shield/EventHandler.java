/*
 * $Id: EventHandler.java 34295 2013-03-17 20:24:07Z dmorris $
 */
package com.untangle.node.shield;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.UDPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.UDPSessionEvent;

class EventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(EventHandler.class);

    public EventHandler( ShieldNodeImpl node )
    {
        super( node );
    }

    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event)
    {
        handleNewSessionRequest(event.sessionRequest(), Protocol.TCP);
    }

    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event)
    {
        handleNewSessionRequest(event.sessionRequest(), Protocol.UDP);
    }

    private void handleNewSessionRequest(IPNewSessionRequest request, Protocol protocol)
    {
        //logger.warn("New session: " + request);
        request.release();
    }
}
