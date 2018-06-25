/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.nio.ByteBuffer;

import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.app.App;
import com.untangle.uvm.vnet.SessionEventHandler;

/**
 * <code>AbstractEventHandler</code> is the abstract base class that provides
 * the default actions that any session event handler will need.
 *
 * By default the event handler methods will just pass the events on to the other side.
 * Subclasses will need to override these methods with the appropriate actions.
 */
public abstract class AbstractEventHandler implements SessionEventHandler
{
    protected AppBase app;

    /**
     * AbstractEventHandler constructor
     * @param app
     */
    protected AbstractEventHandler(App app)
    {
        this.app = (AppBase)app;
    }

    /**
     * AbstractEventHandler constructor
     */
    protected AbstractEventHandler()
    {
        this.app = null;
    }
    
    /**
     * handleTimer handles periodic timer events
     * default behavior: do nothing
     * @param session
     */
    public void handleTimer( AppSession session )
    {
        // do nothing
    }

    /**
     * handleTCPNewSessionRequest handles new TCP session request events
     * default behavior: do nothing
     * @param sessionRequest
     */
    public void handleTCPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        // do nothing
    }

    /**
     * handleTCPNewSession handles new TCP session events
     * default behavior: do nothing
     * @param session
     */
    public void handleTCPNewSession( AppTCPSession session )
    {
        // do nothing
    }

    /**
     * handleTCPClientDataEnd handles the end of data event
     * default behavior: do nothing
     * @param session
     * @param data
     */
    public void handleTCPClientDataEnd( AppTCPSession session, ByteBuffer data )
    {
        // do nothing
    }

    /**
     * handleTCPServerDataEnd handles the end of data event
     * default behavior: do nothing
     * @param session
     * @param data
     */
    public void handleTCPServerDataEnd( AppTCPSession session, ByteBuffer data )
    {
        // do nothing
    }

    /**
     * handleTCPClientFIN handles a FIN event
     * default behavior: send to server
     * @param session
     */
    public void handleTCPClientFIN( AppTCPSession session )
    {
        // propagate shutdown to other side
        session.shutdownServer();
    }

    /**
     * handleTCPServerFIN handles a FIN event
     * default behavior: send to client
     * @param session
     */
    public void handleTCPServerFIN( AppTCPSession session )
    {
        // propagate shutdown to other side
        session.shutdownClient();
    }

    /**
     * handleTCPClientRST handles a reset event
     * default behavior: send to server
     * @param session
     */
    public void handleTCPClientRST( AppTCPSession session )
    {
        // propagate reset to other side
        session.resetServer();
    }

    /**
     * handleTCPServerRST handles a reset event
     * default behavior: send to client
     * @param session
     */
    public void handleTCPServerRST( AppTCPSession session )
    {
        // propagate reset to other side
        session.resetClient();
    }

    /**
     * handleTCPFinalized handle a finalize event
     * default behavior: do nothing
     * @param session
     */
    public void handleTCPFinalized( AppTCPSession session )
    {
        // do nothing
    }

    /**
     * handleTCPComplete handles a finalize event
     * default behavior: do nothing
     * @param session
     */
    public void handleTCPComplete( AppTCPSession session )
    {
        // do nothing
    }

    /**
     * handleTCPClientChunk handles a chunk from the client
     * default behavior: send chunk to server
     * @param session
     * @param data
     */
    public void handleTCPClientChunk( AppTCPSession session, ByteBuffer data )
    {
        session.sendDataToServer( data );
    }

    /**
     * handleTCPServerChunk handles a chunk from the server
     * default behavior: send chunk to client
     * @param session
     * @param data
     */
    public void handleTCPServerChunk( AppTCPSession session, ByteBuffer data )
    {
        session.sendDataToClient( data );
    }

    /**
     * handleTCPClientObject handles an object from the client
     * default behavior: send to server
     * @param session
     * @param obj
     */
    public void handleTCPClientObject( AppTCPSession session, Object obj )
    {
        session.sendObjectToServer( obj );
    }

    /**
     * handleTCPServerObject handles an object from the server
     * default behavior: send to client
     * @param session
     * @param obj
     */
    public void handleTCPServerObject( AppTCPSession session, Object obj )
    {
        session.sendObjectToClient( obj );
    }
    
    /**
     * handleTCPClientWritable handles a writeable event
     * default behavior: do nothing
     * Often used for streaming
     * @param session
     */
    public void handleTCPClientWritable( AppTCPSession session )
    {
        // do nothing
    }

    /**
     * handleTCPServerWritable handles a writeable event
     * default behavior: do nothing
     * Often used for streaming
     * @param session
     */
    public void handleTCPServerWritable( AppTCPSession session )
    {
        // do nothing
    }
    
    /**
     * handleUDPNewSessionRequest handles a new UDP session request
     * default behavior: do nothing
     * @param sessionRequest
     */
    public void handleUDPNewSessionRequest( UDPNewSessionRequest sessionRequest )
    {
        // do nothing
    }

    /**
     * handleUDPNewSession handles a new UDP session event
     * default behavior: do nothing
     * @param session
     */
    public void handleUDPNewSession( AppUDPSession session )
    {
        // do nothing
    }

    /**
     * handleUDPClientExpired handles an UDP expire event
     * default behavior: send to server
     * @param session
     */
    public void handleUDPClientExpired( AppUDPSession session )
    {
        // Current assumption: A single expire will be generated on
        // one side of the pipeline, which will travel across it.
        // Another possibility would be to hit them all at once.  Just
        // go ahead and expire the other side.  The app will override
        // this method if it wants to keep the other side open.
        session.expireServer();
    }

    /**
     * handleUDPServerExpired handles an UDP expire event
     * default behavior: send to client
     * @param session
     */
    public void handleUDPServerExpired( AppUDPSession session )
    {
        // Current assumption: A single expire will be generated on
        // one side of the pipeline, which will travel across it.
        // Another possibility would be to hit them all at once.  Just
        // go ahead and expire the other side.  The app will override
        // this method if it wants to keep the other side open.
        session.expireClient();
    }

    /**
     * handleUDPClientWritable handles a writeable event
     * default behavior: do nothing
     * @param session
     */
    public void handleUDPClientWritable( AppUDPSession session )
    {
        // do nothing
    }

    /**
     * handleUDPServerWritable handles a writeable event
     * default behavior: do nothing
     * @param session
     */
    public void handleUDPServerWritable( AppUDPSession session )
    {
        // do nothing
    }

    /**
     * handleUDPFinalized handles a finalized event
     * default behavior: do nothing
     * @param session
     */
    public void handleUDPFinalized( AppUDPSession session )
    {
        // do nothing
    }

    /**
     * handleUDPComplete handles a finalized event
     * default behavior: do nothing
     * @param session
     */
    public void handleUDPComplete( AppUDPSession session )
    {
        // do nothing
    }

    /**
     * handleUDPClientPacket handles a UDP packet event
     * default behavior: send to server
     * @param session
     * @param data
     * @param header
     */
    public void handleUDPClientPacket( AppUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        session.sendServerPacket( data, header );
    }

    /**
     * handleUDPServerPacket handles a UDP packet event
     * default behavior: send to client
     * @param session
     * @param data
     * @param header
     */
    public void handleUDPServerPacket( AppUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        session.sendClientPacket( data, header );
    }

}
