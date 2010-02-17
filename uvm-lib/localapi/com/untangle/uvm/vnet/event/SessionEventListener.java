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

package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.MPipeException;

/**
 * The listener interface for receiving Vnet events.
 *
 * Note that each handler method is free to rethrow a MPipeException received from
 * sending a request, modifying a session, etc.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public interface SessionEventListener extends java.util.EventListener {

    /**
     * <code>handleTimer</code> is called when the scheduled timer expires.
     *
     * @param event a <code>IPSessionEvent</code> giving which session the timer expired for
     */
    void handleTimer(IPSessionEvent event);


    //////////////////////////////////////////////////////////////////////
    // TCP
    //////////////////////////////////////////////////////////////////////

    /**
     * Called before the session is established (when we get the initial SYN).
     * The node can deny the connection by using TCPNewSessionRequestEvent.reject(), or
     * modifying the client/server addr/port, etc.
     *
     * @param event a <code>TCPNewSessionRequestEvent</code> value
     */
    void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event) throws MPipeException;

    /**
     * Called after the session is established (after the three way handshake
     * is complete, but before any data is transferred).  At this point it is
     * too late to reject or modify the session.  (Of course the session may
     * still be closed at any time)
     *
     * @param event a <code>TCPSessionEvent</code> value
     */
    void handleTCPNewSession(TCPSessionEvent event) throws MPipeException;

    IPDataResult handleTCPClientChunk(TCPChunkEvent event) throws MPipeException;
    IPDataResult handleTCPServerChunk(TCPChunkEvent event) throws MPipeException;


    /**
     * <code>handleTCPServerWritable</code> is called when the write queue to the server has
     * first gone empty.  This is an edge-triggered event that gives the node a chance
     * to write some more bytes.
     *
     * @param event a <code>TCPSessionEvent</code> value
     * @return an <code>IPDataResult</code> value
     * @exception MPipeException if an error occurs
     */
    IPDataResult handleTCPServerWritable(TCPSessionEvent event) throws MPipeException;

    /**
     * <code>handleTCPClientWritable</code> is called when the write queue to the client has
     * first gone empty.  This is an edge-triggered event that gives the node a chance
     * to write some more bytes.
     *
     * @param event a <code>TCPSessionEvent</code> value
     * @return an <code>IPDataResult</code> value
     * @exception MPipeException if an error occurs
     */
    IPDataResult handleTCPClientWritable(TCPSessionEvent event) throws MPipeException;

    /**
     * <code>handleTCPClientDataEnd</code> is called just as the first EOF (Shutdown) is read from
     * the client.  This gives the node a chance to send out any buffered data/etc.
     * 
     * The function may return null, which means to do nothing.
     * 
     * If the function returns an IPDataResult, the bufsToClient and bufsToServer are added to the
     * respective outgoing queues.  The readBuffer is ignored.
     *
     * handleTCPClientFIN is called just after this.
     */
    IPDataResult handleTCPClientDataEnd(TCPChunkEvent event) throws MPipeException;

    /**
     * <code>handleTCPClientFIN</code> is called when the first EOF (Shutdown) is read from
     * the client.  This will also happen if we ourselves have shutdown (sent a FIN to)
     * the client and we have waited the timeout number of seconds (usually ~30? XXX)
     * for the FIN response from the client.  (FIN/ACKs are ignored.)
     * The default action, from <code>AbstractEventHandler</code>
     * is to call <code>shutdownServer</code>.
     *
     * This is called just after handleTcpClientDataEnd.
     *
     * @param event a <code>TCPSessionEvent</code> value
     * @exception MPipeException if an error occurs
     */
    void handleTCPClientFIN(TCPSessionEvent event) throws MPipeException;

    /**
     * <code>handleTCPServerDataEnd</code> is called just as the first EOF (Shutdown) is read from
     * the server.  This gives the node a chance to send out any buffered data/etc.
     * 
     * The function may return null, which means to do nothing.
     * 
     * If the function returns an IPDataResult, the bufsToServer and bufsToServer are added to the
     * respective outgoing queues.  The readBuffer is ignored.
     *
     * handleTCPServerFIN is called just after this.
     */
    IPDataResult handleTCPServerDataEnd(TCPChunkEvent event) throws MPipeException;

    /**
     * <code>handleTCPServerFIN</code> is called when the first EOF (Shutdown) is read from
     * the server.  This will also happen if we ourselves have shutdown (sent a FIN to)
     * the server and we have waited the timeout number of seconds (usually ~30? XXX)
     * for the FIN response from the server.  (FIN/ACKs are ignored.)
     * The default action, from <code>AbstractEventHandler</code>
     * is to call <code>shutdownClient</code>.
     *
     * This is called just after handleTcpServerDataEnd.
     *
     * @param event a <code>TCPSessionEvent</code> value
     * @exception MPipeException if an error occurs
     */
    void handleTCPServerFIN(TCPSessionEvent event) throws MPipeException;



    /**
     * <code>handleTCPClientRST</code> is called when the first RST (Reset) is read from
     * the client. 
     * The default action, from <code>AbstractEventHandler</code>
     * is to call <code>resetServer</code>.
     *
     * Note that reset is different from shutdown in that it closes both directions of
     * the client without waiting for a response (FIN or RST)) from the client.  So there
     * is no need to pass through a second reset as there is with shutdown.
     *
     * @param event a <code>TCPSessionEvent</code> giving the session
     */
    void handleTCPClientRST(TCPSessionEvent event) throws MPipeException;

    /**
     * <code>handleTCPServerReset</code> is called when the first RST (Reset) is read from
     * the server.
     * The default action, from <code>AbstractEventHandler</code>
     * is to call <code>resetClient</code>.
     *
     * Note that reset is different from shutdown in that it closes both directions of
     * the server without waiting for a response (FIN or RST)) from the server.  So there
     * is no need to pass through a second reset as there is with shutdown.
     *
     * @param event a <code>TCPSessionEvent</code> giving the session
     */
    void handleTCPServerRST(TCPSessionEvent event) throws MPipeException;

    /**
     * As a convenience, <code>handleTCPFinalized</code> is called once both
     * the client and server are completely closed.  This happens when two shutdowns
     * have completed (== client to server FIN and server to client FIN or vice-versa),
     * or when one reset (== either direction RST) has completed. This is the last time the
     * session may be used, after this handler returns the session is
     * destroyed.  Practically this is called just after the client issues a
     * <code>shutdown</code> or <code>reset</code> to the last open output half-channel.
     *
     * Special cases:
     *  1) If the session is released at NewSession time, it is never Finalized
     *  2) If the session is released at a later time, it is never Finalized
     *
     * @param event a <code>TCPSessionEvent</code> giving the session
     */
    void handleTCPFinalized(TCPSessionEvent event)  throws MPipeException;

    /**
     * <code>handleTCPComplete</code> is delivered when a session is:
     *   a released session
     *   that needs finalization
     *   where the pipeline has been registered (server and client connected, or session rejected)
     *
     * It is delivered just after the pipeline endpoints have been registered
     * but before vectoring has begun.
     *
     * @param event a <code>TCPSessionEvent</code> value
     * @exception MPipeException if an error occurs
     */
    void handleTCPComplete(TCPSessionEvent event) throws MPipeException;


    //////////////////////////////////////////////////////////////////////
    // UDP
    //////////////////////////////////////////////////////////////////////

    // Note that the Packet handlers are not, in general, free to mess with the event's packet
    // position/limit, as these will be used by the default handler when sending out the packet. XX
    void handleUDPClientPacket(UDPPacketEvent event) throws MPipeException;
    void handleUDPServerPacket(UDPPacketEvent event) throws MPipeException;

    /**
     * Called after the session is established (after the new session request
     * is complete, but before any data is transferred).  At this point it is
     * too late to reject or modify the session.  (Of course the session may
     * still be closed/released at any time)
     *
     * @param event a <code>UDPSessionEvent</code> value
     */
    void handleUDPNewSession(UDPSessionEvent event) throws MPipeException;

    /**
     * Called before the session is established (when we get the initial packet).
     * The node can deny the session by using UDPNewSessionRequestEvent.reject(), or
     * modifying the client/server addr/port, etc.
     *
     * @param event a <code>TCPNewSessionRequestEvent</code> value
     */
    void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event) throws MPipeException;

    void handleUDPClientExpired(UDPSessionEvent event)  throws MPipeException;

    void handleUDPServerExpired(UDPSessionEvent event)  throws MPipeException;

    void handleUDPClientError(UDPErrorEvent event)  throws MPipeException;

    void handleUDPServerError(UDPErrorEvent event)  throws MPipeException;

    /**
     * <code>handleUDPServerWritable</code> is called when the write queue to the server has
     * first gone empty.  This is an edge-triggered event that gives the node a chance
     * to write some more packets.
     *
     * @param event a <code>UDPSessionEvent</code> value
     * @return an <code>IPDataResult</code> value
     * @exception MPipeException if an error occurs
     */
    void handleUDPServerWritable(UDPSessionEvent event) throws MPipeException;

    /**
     * <code>handleUDPClientWritable</code> is called when the write queue to the client has
     * first gone empty.  This is an edge-triggered event that gives the node a chance
     * to write some more packets.
     *
     * @param event a <code>UDPSessionEvent</code> value
     * @return an <code>IPDataResult</code> value
     * @exception MPipeException if an error occurs
     */
    void handleUDPClientWritable(UDPSessionEvent event) throws MPipeException;

    /**
     * <code>handleUDPFinalized</code> is called once both the client and server have
     * expired.  This is the last time the session may be used, after this
     * handler returns the session is destroyed.
     *
     * Special cases:
     *  1) If the session is released at NewSession time, it is never Finalized
     *  2) If the session is released at a later time, it is never Finalized
     *  3) If the session is not in NORMAL mode, it is still Finalized, either
     *     when the task(s) have returned (if closeWhenDone is true, or both sides
     *     are closed at that time), or at whatever later time (now that we're back
     *     in NORMAL mode) both sides have closed.
     *
     * @param event a <code>UDPSessionEvent</code> giving the session
     */
    void handleUDPFinalized(UDPSessionEvent event)  throws MPipeException;

    /**
     * <code>handleUDPComplete</code> is delivered when a session is:
     *   a released session
     *   that needs finalization
     *   where the pipeline has been registered (server and client connected, or session rejected)
     *
     * It is delivered just after the pipeline endpoints have been registered
     * but before vectoring has begun.
     *
     * @param event a <code>UDPSessionEvent</code> value
     * @exception MPipeException if an error occurs
     */
    void handleUDPComplete(UDPSessionEvent event) throws MPipeException;
}

