/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.tapi;

import com.untangle.mvvm.tapi.event.*;
import com.untangle.mvvm.tran.Transform;


public abstract class AbstractEventHandler implements SessionEventListener {

    protected AbstractTransform xform;

    protected AbstractEventHandler(Transform xform)
    {
        // XXX
        this.xform = (AbstractTransform)xform;
    }

    public void handleTimer(IPSessionEvent event)
    {
    }

    protected long incrementCount(int i)
    {
        return xform.incrementCount(i, 1);
    }

    protected long incrementCount(int i, long delta)
    {
        return xform.incrementCount(i, delta);
    }

    /*
    // This should be enhanced. XXX
    public void handleMPipeHeartbeatRequest(MPipeHeartbeatRequestEvent event)
    {
    // Check to make sure the client health is good.  XXX
    boolean ok = true;
    MPipe mPipe = event.mPipe();
    try {
    // What does it mean for exection from here? XXX

    if (ok) {
    mPipe.sendOkHeartbeat();
    } else {
    // XXX
    mPipe.sendErrHeartbeat("foo", MNPConstants.ERROR, "");
    }
    } catch (MPipeException x) {
    // XXX
    }
    }
    */


    //////////////////////////////////////////////////////////////////////
    // TCP
    //////////////////////////////////////////////////////////////////////

    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event)
        throws MPipeException
    {
        /* accept */
    }

    public void handleTCPNewSession(TCPSessionEvent event)
        throws MPipeException
    {
        /* ignore */
    }

    public IPDataResult handleTCPClientDataEnd(TCPChunkEvent event)
    {
        /* ignore */
        return null;
    }

    public void handleTCPClientFIN(TCPSessionEvent event)
        throws MPipeException
    {
        // Just go ahead and shut down the other side.  The transform will override
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
        throws MPipeException
    {
        // Just go ahead and shut down the other side.  The transform will override
        // this method if it wants to keep the other side open.
        TCPSession sess = event.session();
        sess.shutdownClient();
    }

    public void handleTCPClientRST(TCPSessionEvent event)
        throws MPipeException
    {
        // Just go ahead and reset the other side.  The transform will override
        // this method if it wants to keep the other side open.
        TCPSession sess = event.session();
        sess.resetServer();
    }

    public void handleTCPServerRST(TCPSessionEvent event)
        throws MPipeException
    {
        // Just go ahead and reset the other side.  The transform will override
        // this method if it wants to keep the other side open.
        TCPSession sess = event.session();
        sess.resetClient();
    }

    public void handleTCPFinalized(TCPSessionEvent event)
        throws MPipeException
    {
    }

    public void handleTCPComplete(TCPSessionEvent event)
        throws MPipeException
    {
    }

    public IPDataResult handleTCPClientChunk(TCPChunkEvent event)
        throws MPipeException
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
        throws MPipeException
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
        throws MPipeException
    {
        // Default writes nothing more.
        return IPDataResult.SEND_NOTHING;
    }

    public IPDataResult handleTCPClientWritable(TCPSessionEvent event)
        throws MPipeException
    {
        // Default writes nothing more.
        return IPDataResult.SEND_NOTHING;
    }


    //////////////////////////////////////////////////////////////////////
    // UDP
    //////////////////////////////////////////////////////////////////////

    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event)
        throws MPipeException
    {
        /* accept */
    }

    public void handleUDPNewSession(UDPSessionEvent event)
        throws MPipeException
    {
        /* ignore */
    }

    public void handleUDPClientExpired(UDPSessionEvent event)
        throws MPipeException
    {
        // Current assumption: A single expire will be generated on one side of the pipeline,
        // which will travel across it.  Another possibility would be to hit them all at once.
        // Just go ahead and expire the other side.  The transform will override
        // this method if it wants to keep the other side open.
        UDPSession sess = event.session();
        sess.expireServer();
    }

    public void handleUDPServerExpired(UDPSessionEvent event)
        throws MPipeException
    {
        // Current assumption: A single expire will be generated on one side of the pipeline,
        // which will travel across it.  Another possibility would be to hit them all at once.
        // Just go ahead and expire the other side.  The transform will override
        // this method if it wants to keep the other side open.
        UDPSession sess = event.session();
        sess.expireClient();
    }

    public void handleUDPServerWritable(UDPSessionEvent event)
        throws MPipeException
    {
        // Default writes nothing more.
    }

    public void handleUDPClientWritable(UDPSessionEvent event)
        throws MPipeException
    {
        // Default writes nothing more.
    }

    public void handleUDPFinalized(UDPSessionEvent event)
        throws MPipeException
    {
    }

    public void handleUDPComplete(UDPSessionEvent event)
        throws MPipeException
    {
    }


    public void handleUDPClientPacket(UDPPacketEvent event)
        throws MPipeException
    {
        UDPSession session = event.session();
        byte serverState = session.serverState();
        // Default just sends the bytes onwards if the output is open.
        if (serverState == IPSessionDesc.OPEN)
            session.sendServerPacket(event.packet(), event.header());
    }

    public void handleUDPServerPacket(UDPPacketEvent event)
        throws MPipeException
    {
        UDPSession session = event.session();
        byte clientState = session.clientState();
        // Default just sends the bytes onwards if the output is open.
        if (clientState == IPSessionDesc.OPEN)
            session.sendClientPacket(event.packet(), event.header());
    }

    public void handleUDPClientError(UDPErrorEvent event)
        throws MPipeException
    {
        // Default just sends the error onwards.
        UDPSession sess = event.session();
        sess.sendServerError(event.getErrorType(), event.getErrorCode(), event.packet(), event.getErrorSource(), event.header());
    }

    public void handleUDPServerError(UDPErrorEvent event)
        throws MPipeException
    {
        // Default just sends the error onwards.
        UDPSession sess = event.session();
        sess.sendClientError(event.getErrorType(), event.getErrorCode(), event.packet(), event.getErrorSource(), event.header());
    }

}
