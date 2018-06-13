/**
 * $Id$
 */
package com.untangle.app.ftp;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.EndMarkerToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.ReleaseToken;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.AppTCPSession;

/**
 * State machine for FTP traffic.
 *
 */
public abstract class FtpEventHandler extends AbstractEventHandler
{
    /**
     * Used to obtain the control session that opened the data session on the
     * given port.
     */
    private static final Map<InetSocketAddress, Long> ctlSessionIdByDataSocket = new ConcurrentHashMap<InetSocketAddress, Long>();
    
    /**
     * Create a new FtpEventHandler
     */
    protected FtpEventHandler() {}

    /**
     * doCommand handles FtpCommand tokens (it just passes them by default)
     * Override for different behavior
     * @param session
     * @param command
     */
    protected void doCommand( AppTCPSession session, FtpCommand command )
    {
        session.sendObjectToServer( command );
    }

    /**
     * doReply handles FtpReply tokens (it just passes them by default)
     * Override for different behavior
     * @param session
     * @param reply
     */
    protected void doReply( AppTCPSession session, FtpReply reply )
    {
        session.sendObjectToClient( reply );
    }

    /**
     * doClientData handles client data (it just passes it by default)
     * Override for different behavior
     * @param session
     * @param chunk
     */
    protected void doClientData( AppTCPSession session, ChunkToken chunk )
    {
        session.sendObjectToServer( chunk );
    }

    /**
     * doServerData handles server data (it just passes it by default)
     * Override for different behavior
     * @param session
     * @param chunk
     */
    protected void doServerData( AppTCPSession session, ChunkToken chunk )
    {
        session.sendObjectToClient( chunk );
    }

    /**
     * doClientDataEnd - does nothing by default
     * Override for different behavior
     * @param session
     */
    protected void doClientDataEnd( AppTCPSession session ) { }
    
    /**
     * doServerDataEnd - does nothing by default
     * Override for different behavior
     * @param session
     */
    protected void doServerDataEnd( AppTCPSession session ) { }

    /**
     * handleTCPServerObject - handles a token from the server
     * @param session
     * @param obj
     */
    @Override
    public final void handleTCPServerObject( AppTCPSession session, Object obj )
    {
        Token token = (Token) obj;
        if (token instanceof ReleaseToken) {
            handleTCPFinalized( session );
            session.sendObjectToClient( token );
            session.release();
            return;
        }

        Fitting serverFitting = session.pipelineConnector().getOutputFitting();

        if (Fitting.FTP_CTL_TOKENS == serverFitting) {
            doReply( session, (FtpReply)token );
            return;
        } else if (Fitting.FTP_DATA_TOKENS == serverFitting) {
            if (token instanceof EndMarkerToken) {
                session.sendObjectToClient( EndMarkerToken.MARKER );
                return;
            } else if (token instanceof ChunkToken) {
                doServerData( session, (ChunkToken)token );
                return;
            } else {
                throw new RuntimeException("bad token: " + token);
            }
        } else {
            throw new IllegalStateException("bad fitting: " + serverFitting);
        }
    }

    /**
     * handleTCPClientObject - handles a token from the client
     * @param session
     * @param obj
     */
    @Override
    public final void handleTCPClientObject( AppTCPSession session, Object obj )
    {
        Token token = (Token) obj;
        if (token instanceof ReleaseToken) {
            handleTCPFinalized( session );
            session.sendObjectToServer( token );
            session.release();
            return;
        }

        Fitting clientFitting = session.pipelineConnector().getInputFitting();

        if (Fitting.FTP_CTL_TOKENS == clientFitting) {
            doCommand( session, (FtpCommand)token );
            return;
        } else if (Fitting.FTP_DATA_TOKENS == clientFitting) {
            if (token instanceof EndMarkerToken) {
                session.sendObjectToServer( EndMarkerToken.MARKER );
                return;
            } else if (token instanceof ChunkToken) {
                doClientData( session, (ChunkToken)token );
                return;
            } else {
                throw new RuntimeException("bad token: " + token);
            }
        } else {
            throw new IllegalStateException("bad fitting: " + clientFitting);
        }
    }
    
    /**
     * handleTCPClientFIN - handle FIN from the client (it just passes by default)
     * Override for different behavior
     * @param session
     */
    @Override
    public final void handleTCPClientFIN( AppTCPSession session )
    {
        doClientDataEnd( session );
    }

    /**
     * handleTCPServerFIN - handle a FIN from the server (it just passes by default)
     * Override for different behavior
     * @param session <doc>
     */
    @Override
    public final void handleTCPServerFIN( AppTCPSession session )
    {
        doServerDataEnd( session );
    }
    
    /**
     * Add a dataSocket that will be opened by this control session
     * @param dataSocket
     * @param ctlSessionId
     */
    public static void addDataSocket(InetSocketAddress dataSocket, Long ctlSessionId)
    {
        ctlSessionIdByDataSocket.put(dataSocket, ctlSessionId);
    }

    /**
     * removeDataSocket - Remove the mapping of this data socket
     * @param dataSocket
     * @return ctlSessionId
     */
    public static Long removeDataSocket(InetSocketAddress dataSocket)
    {
        if (ctlSessionIdByDataSocket.containsKey(dataSocket)) {
            return ctlSessionIdByDataSocket.remove(dataSocket);
        }
        return null;
    }
    
    /**
     * Remove all mappings of this control session
     * @param ctlSessionId
     */
    public static void removeDataSockets(long ctlSessionId)
    {
        Set<InetSocketAddress> set = new HashSet<InetSocketAddress>(ctlSessionIdByDataSocket.keySet());
        for (InetSocketAddress dataSocket : set){
            if (ctlSessionIdByDataSocket.get(dataSocket).longValue() == ctlSessionId)
                ctlSessionIdByDataSocket.remove(dataSocket);
        }
    }
}
