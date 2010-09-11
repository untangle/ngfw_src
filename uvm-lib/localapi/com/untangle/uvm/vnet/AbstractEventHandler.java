/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.vnet;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.vnet.event.IPDataResult;
import com.untangle.uvm.vnet.event.IPSessionEvent;
import com.untangle.uvm.vnet.event.SessionEventListener;
import com.untangle.uvm.vnet.event.TCPChunkEvent;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.UDPErrorEvent;
import com.untangle.uvm.vnet.event.UDPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.UDPPacketEvent;
import com.untangle.uvm.vnet.event.UDPSessionEvent;


/**
 * <code>AbstractEventHandler</code> is the abstract base class that provides
 * the default actions that any node event handler will need.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public abstract class AbstractEventHandler implements SessionEventListener {

    protected AbstractNode xform;

    protected AbstractEventHandler(Node xform)
    {
        this.xform = (AbstractNode)xform;
    }

    public void handleTimer(IPSessionEvent event)
    {
    }

    //////////////////////////////////////////////////////////////////////
    // TCP
    //////////////////////////////////////////////////////////////////////

    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event)
        
    {
        /* accept */
    }

    public void handleTCPNewSession(TCPSessionEvent event)
        
    {
        /* ignore */
    }

    public IPDataResult handleTCPClientDataEnd(TCPChunkEvent event)
    {
        /* ignore */
        return null;
    }

    public void handleTCPClientFIN(TCPSessionEvent event)
        
    {
        // Just go ahead and shut down the other side.  The node will override
        // this method if it wants to keep the other side open.
        TCPSession sess = event.session();
        sess.shutdownServer();
    }

    public IPDataResult handleTCPServerDataEnd(TCPChunkEvent event)
    {
        /* ignore */
        return null;
    }

    public void handleTCPServerFIN(TCPSessionEvent event)
        
    {
        // Just go ahead and shut down the other side.  The node will override
        // this method if it wants to keep the other side open.
        TCPSession sess = event.session();
        sess.shutdownClient();
    }

    public void handleTCPClientRST(TCPSessionEvent event)
        
    {
        // Just go ahead and reset the other side.  The node will override
        // this method if it wants to keep the other side open.
        TCPSession sess = event.session();
        sess.resetServer();
    }

    public void handleTCPServerRST(TCPSessionEvent event)
        
    {
        // Just go ahead and reset the other side.  The node will override
        // this method if it wants to keep the other side open.
        TCPSession sess = event.session();
        sess.resetClient();
    }

    public void handleTCPFinalized(TCPSessionEvent event)
        
    {
    }

    public void handleTCPComplete(TCPSessionEvent event)
        
    {
    }

    public IPDataResult handleTCPClientChunk(TCPChunkEvent event)
        
    {
        TCPSession session = event.session();
        byte serverState = session.serverState();
        // Default just sends the bytes onwards if the output is open.
        if (serverState == IPSessionDesc.OPEN || serverState == TCPSessionDesc.HALF_OPEN_OUTPUT)
            return IPDataResult.PASS_THROUGH;
        else
            return IPDataResult.DO_NOT_PASS;
    }

    public IPDataResult handleTCPServerChunk(TCPChunkEvent event)
        
    {
        TCPSession session = event.session();
        byte clientState = session.clientState();
        // Default just sends the bytes onwards if the output is open.
        if (clientState == IPSessionDesc.OPEN || clientState == TCPSessionDesc.HALF_OPEN_OUTPUT)
            return IPDataResult.PASS_THROUGH;
        else
            return IPDataResult.DO_NOT_PASS;
    }

    public IPDataResult handleTCPServerWritable(TCPSessionEvent event)
        
    {
        // Default writes nothing more.
        return IPDataResult.SEND_NOTHING;
    }

    public IPDataResult handleTCPClientWritable(TCPSessionEvent event)
        
    {
        // Default writes nothing more.
        return IPDataResult.SEND_NOTHING;
    }


    //////////////////////////////////////////////////////////////////////
    // UDP
    //////////////////////////////////////////////////////////////////////

    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event)
        
    {
        /* accept */
    }

    public void handleUDPNewSession(UDPSessionEvent event)
        
    {
        /* ignore */
    }

    public void handleUDPClientExpired(UDPSessionEvent event)
        
    {
        // Current assumption: A single expire will be generated on
        // one side of the pipeline, which will travel across it.
        // Another possibility would be to hit them all at once.  Just
        // go ahead and expire the other side.  The node will override
        // this method if it wants to keep the other side open.
        UDPSession sess = event.session();
        sess.expireServer();
    }

    public void handleUDPServerExpired(UDPSessionEvent event)
        
    {
        // Current assumption: A single expire will be generated on
        // one side of the pipeline, which will travel across it.
        // Another possibility would be to hit them all at once.  Just
        // go ahead and expire the other side.  The node will override
        // this method if it wants to keep the other side open.
        UDPSession sess = event.session();
        sess.expireClient();
    }

    public void handleUDPServerWritable(UDPSessionEvent event)
        
    {
        // Default writes nothing more.
    }

    public void handleUDPClientWritable(UDPSessionEvent event)
        
    {
        // Default writes nothing more.
    }

    public void handleUDPFinalized(UDPSessionEvent event)
        
    {
    }

    public void handleUDPComplete(UDPSessionEvent event)
        
    {
    }


    public void handleUDPClientPacket(UDPPacketEvent event)
        
    {
        UDPSession session = event.session();
        byte serverState = session.serverState();
        // Default just sends the bytes onwards if the output is open.
        if (serverState == IPSessionDesc.OPEN)
            session.sendServerPacket(event.packet(), event.header());
    }

    public void handleUDPServerPacket(UDPPacketEvent event)
        
    {
        UDPSession session = event.session();
        byte clientState = session.clientState();
        // Default just sends the bytes onwards if the output is open.
        if (clientState == IPSessionDesc.OPEN)
            session.sendClientPacket(event.packet(), event.header());
    }

    public void handleUDPClientError(UDPErrorEvent event)
        
    {
        // Default just sends the error onwards.
        UDPSession sess = event.session();
        sess.sendServerError(event.getErrorType(), event.getErrorCode(), event.packet(), event.getErrorSource(), event.header());
    }

    public void handleUDPServerError(UDPErrorEvent event)
        
    {
        // Default just sends the error onwards.
        UDPSession sess = event.session();
        sess.sendClientError(event.getErrorType(), event.getErrorCode(), event.packet(), event.getErrorSource(), event.header());
    }

}
