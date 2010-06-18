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

package com.untangle.node.sigma;

import com.untangle.uvm.vnet.*;
import com.untangle.uvm.vnet.event.*;
import com.untangle.uvm.node.Node;
import org.apache.log4j.Logger;

public class EventHandler extends AbstractEventHandler
{
    private class SessionInfo 
    {
    }

    public EventHandler(Node node)
    {
        super(node);
    }

    public void handleTCPNewSession (TCPSessionEvent event)
    {
        TCPSession sess = event.session();
        SessionInfo sessInfo = new SessionInfo();
        sess.attach(sessInfo);
    }

    public void handleUDPNewSession (UDPSessionEvent event)
    {
        UDPSession sess = event.session();

        SessionInfo sessInfo = new SessionInfo();
        sess.attach(sessInfo);
    }

    public IPDataResult handleTCPClientChunk (TCPChunkEvent e)
    {
        return IPDataResult.PASS_THROUGH;
    }

    public IPDataResult handleTCPServerChunk (TCPChunkEvent e)
    {
        return IPDataResult.PASS_THROUGH;
    }

    public void handleUDPClientPacket (UDPPacketEvent e)
        throws MPipeException
    {
        UDPSession sess = e.session();
        sess.sendClientPacket(e.packet(), e.header());
    }

    public void handleUDPServerPacket (UDPPacketEvent e)
        throws MPipeException
    {
        UDPSession sess = e.session();
        sess.sendClientPacket(e.packet(), e.header());
    }

}
