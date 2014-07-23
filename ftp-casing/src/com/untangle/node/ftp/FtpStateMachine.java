/**
 * $Id$
 */
package com.untangle.node.ftp;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.untangle.node.token.ChunkToken;
import com.untangle.node.token.EndMarkerToken;
import com.untangle.node.token.Token;
import com.untangle.node.token.ReleaseToken;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * State machine for FTP traffic.
 *
 */
public abstract class FtpStateMachine extends AbstractEventHandler
{
    /**
     * Used to obtain the control session that opened the data session on the
     * given port.
     */
    private static final Map<InetSocketAddress, Long> ctlSessionIdByDataSocket = new ConcurrentHashMap<InetSocketAddress, Long>();
    
    protected FtpStateMachine() {}

    protected void doCommand( NodeTCPSession session, FtpCommand command )
    {
        session.sendObjectToServer( command );
    }

    protected void doReply( NodeTCPSession session, FtpReply reply )
    {
        session.sendObjectToClient( reply );
    }

    protected void doClientData( NodeTCPSession session, ChunkToken chunk )
    {
        session.sendObjectToServer( chunk );
    }

    protected void doClientDataEnd( NodeTCPSession session ) { }

    protected void doServerData( NodeTCPSession session, ChunkToken chunk )
    {
        session.sendObjectToClient( chunk );
    }

    protected void doServerDataEnd( NodeTCPSession session ) { }

    @Override
    public void handleTCPServerObject( NodeTCPSession session, Object obj )
    {
        Token token = (Token) obj;
        if (token instanceof ReleaseToken) {
            handleTCPFinalized( session );
            session.release();
        }

        handleServerToken( session, token );
        return;
    }

    @Override
    public void handleTCPClientObject( NodeTCPSession session, Object obj )
    {
        Token token = (Token) obj;
        if (token instanceof ReleaseToken) {
            handleTCPFinalized( session );
            session.release();
        }

        handleClientToken( session, token );
        return;
    }
    
    public void handleClientToken( NodeTCPSession session, Token token )
    {
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

    public void handleServerToken( NodeTCPSession session, Token token )
    {
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

    @Override
    public void handleTCPClientFIN( NodeTCPSession session )
    {
        doClientDataEnd( session );
    }

    @Override
    public void handleTCPServerFIN( NodeTCPSession session )
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
     * Remove the mapping of this data socket
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
