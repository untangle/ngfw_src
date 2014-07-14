/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.NodeUDPSession;
import com.untangle.uvm.vnet.UDPNewSessionRequest;
import com.untangle.uvm.vnet.IPPacketHeader;

/**
 * The listener interface for receiving Vnet events.
 *
 * Note that each handler method is free to rethrow an Exception received from
 * sending a request, modifying a session, etc.
 */
public interface SessionEventListener extends java.util.EventListener
{

    /**
     * <code>handleTimer</code> is called when the scheduled timer expires.
     *
     * @param event a <code>IPSessionEvent</code> giving which session the timer expired for
     */
    void handleTimer( NodeSession session );

    /**
     * TCP
     */

    /**
     * Called before the session is established (when we get the initial SYN).
     * The node can deny the connection by using TCPNewSessionRequestEvent.reject(), or
     * modifying the client/server addr/port, etc.
     *
     * @param event a <code>TCPNewSessionRequestEvent</code> value
     */
    void handleTCPNewSessionRequest( TCPNewSessionRequest sessionRequest );

    /**
     * Called after the session is established (after the three way handshake
     * is complete, but before any data is transferred).  At this point it is
     * too late to reject or modify the session.  (Of course the session may
     * still be closed at any time)
     *
     * @param event a <code>NodeTCPSession</code> value
     */
    void handleTCPNewSession( NodeTCPSession session );

    /**
     * Called when data arrives from the client side
     */
    void handleTCPClientChunk( NodeTCPSession session, ByteBuffer data );

    /**
     * Called when data arrives from the server side
     */
    void handleTCPServerChunk( NodeTCPSession session, ByteBuffer data );

    /**
     * Called an object arrives from the client side
     */
    void handleTCPClientObject( NodeTCPSession session, Object obj );

    /**
     * Called an object arrives from the server side
     */
    void handleTCPServerObject( NodeTCPSession session, Object obj );
    
    /**
     * <code>handleTCPServerWritable</code> is called when the write queue to the server has
     * first gone empty.  This is an edge-triggered event that gives the node a chance
     * to write some more bytes.
     *
     * @param event a <code>NodeTCPSession</code> value
     * @return an <code>void</code> value
     * @exception Exception if an error occurs
     */
    void handleTCPServerWritable( NodeTCPSession session );

    /**
     * <code>handleTCPClientWritable</code> is called when the write queue to the client has
     * first gone empty.  This is an edge-triggered event that gives the node a chance
     * to write some more bytes.
     *
     * @param event a <code>NodeTCPSession</code> value
     * @return an <code>void</code> value
     * @exception Exception if an error occurs
     */
    void handleTCPClientWritable( NodeTCPSession session );

    /**
     * <code>handleTCPClientDataEnd</code> is called just as the first EOF (Shutdown) is read from
     * the client.  This gives the node a chance to send out any buffered data/etc.
     * 
     * The function may return null, which means to do nothing.
     * 
     * If the function returns an void, the bufsToClient and bufsToServer are added to the
     * respective outgoing queues.  The readBuffer is ignored.
     *
     * handleTCPClientFIN is called just after this.
     */
    void handleTCPClientDataEnd( NodeTCPSession session, ByteBuffer data );

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
     * @param event a <code>NodeTCPSession</code> value
     * @exception Exception if an error occurs
     */
    void handleTCPClientFIN( NodeTCPSession session );

    /**
     * <code>handleTCPServerDataEnd</code> is called just as the first EOF (Shutdown) is read from
     * the server.  This gives the node a chance to send out any buffered data/etc.
     * 
     * The function may return null, which means to do nothing.
     * 
     * If the function returns an void, the bufsToServer and bufsToServer are added to the
     * respective outgoing queues.  The readBuffer is ignored.
     *
     * handleTCPServerFIN is called just after this.
     */
    void handleTCPServerDataEnd( NodeTCPSession session, ByteBuffer data );

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
     * @param event a <code>NodeTCPSession</code> value
     * @exception Exception if an error occurs
     */
    void handleTCPServerFIN( NodeTCPSession session );

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
     * @param event a <code>NodeTCPSession</code> giving the session
     */
    void handleTCPClientRST( NodeTCPSession session );

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
     * @param event a <code>NodeTCPSession</code> giving the session
     */
    void handleTCPServerRST( NodeTCPSession session );

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
     * @param event a <code>NodeTCPSession</code> giving the session
     */
    void handleTCPFinalized( NodeTCPSession session );

    /**
     * <code>handleTCPComplete</code> is delivered when a session completed on both sides
     *
     * It is delivered just after the pipeline endpoints have been registered
     * but before vectoring has begun.
     *
     * @param event a <code>NodeTCPSession</code> value
     * @exception Exception if an error occurs
     */
    void handleTCPComplete( NodeTCPSession session );

    /**
     * UDP
     */

    /**
     * Note that the Packet handlers are not, in general, free to mess with the event's packet
     * position/limit, as these will be used by the default handler when sending out the packet.
     */
    void handleUDPClientPacket( NodeUDPSession session, ByteBuffer data, IPPacketHeader header );
    
    /**
     * Note that the Packet handlers are not, in general, free to mess with the event's packet
     * position/limit, as these will be used by the default handler when sending out the packet.
     */
    void handleUDPServerPacket( NodeUDPSession session, ByteBuffer data, IPPacketHeader header );

    /**
     * Called after the session is established (after the new session request
     * is complete, but before any data is transferred).  At this point it is
     * too late to reject or modify the session.  (Of course the session may
     * still be closed/released at any time)
     *
     * @param event a <code>NodeUDPSession</code> value
     */
    void handleUDPNewSession( NodeUDPSession session );

    /**
     * Called before the session is established (when we get the initial packet).
     * The node can deny the session by using UDPNewSessionRequestEvent.reject(), or
     * modifying the client/server addr/port, etc.
     *
     * @param event a <code>TCPNewSessionRequestEvent</code> value
     */
    void handleUDPNewSessionRequest( UDPNewSessionRequest sessionRequest );

    /**
     * Similar to a FIN event, but its just a timeout event, not actually
     * something received from the client
     */
    void handleUDPClientExpired( NodeUDPSession session );

    /**
     * Similar to a FIN event, but its just a timeout event, not actually
     * something received from the server
     */
    void handleUDPServerExpired( NodeUDPSession session );

    /**
     * <code>handleUDPServerWritable</code> is called when the write queue to the server has
     * first gone empty.  This is an edge-triggered event that gives the node a chance
     * to write some more packets.
     *
     * @param event a <code>NodeUDPSession</code> value
     * @exception Exception if an error occurs
     */
    void handleUDPServerWritable( NodeUDPSession session );

    /**
     * <code>handleUDPClientWritable</code> is called when the write queue to the client has
     * first gone empty.  This is an edge-triggered event that gives the node a chance
     * to write some more packets.
     *
     * @param event a <code>NodeUDPSession</code> value
     * @exception Exception if an error occurs
     */
    void handleUDPClientWritable( NodeUDPSession session );

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
     * @param event a <code>NodeUDPSession</code> giving the session
     */
    void handleUDPFinalized( NodeUDPSession session );

    /**
     * <code>handleUDPComplete</code> is delivered when a session is completed on both sides
     *   where the pipeline has been registered (server and client connected, or session rejected)
     *
     * It is delivered just after the pipeline endpoints have been registered
     * but before vectoring has begun.
     *
     * @param event a <code>NodeUDPSession</code> value
     * @exception Exception if an error occurs
     */
    void handleUDPComplete( NodeUDPSession session );
}

