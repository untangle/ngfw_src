/**
 * $Id$
 */
package com.untangle.node.ftp;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.untangle.node.token.AbstractTokenHandler;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.EndMarker;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenException;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * State machine for FTP traffic.
 *
 */
public abstract class FtpStateMachine extends AbstractTokenHandler
{
    //private final Fitting clientFitting;
    //private final Fitting serverFitting;

    // public final NodeTCPSession session;
    /**
     * Used to obtain the control session that opened the data session on the
     * given port.
     */
    private static final Map<InetSocketAddress, Long> ctlSessionIdByDataSocket = new ConcurrentHashMap<InetSocketAddress, Long>();
    
    // constructors -----------------------------------------------------------

    protected FtpStateMachine()
    {
        // this.session = session;
        // clientFitting = session.pipelineConnector().getInputFitting();
        // serverFitting = session.pipelineConnector().getOutputFitting();
    }

    // protected methods ------------------------------------------------------

    protected void doCommand( NodeTCPSession session, FtpCommand command ) throws TokenException
    {
        session.sendObjectToServer( command );
    }

    protected void doReply( NodeTCPSession session, FtpReply reply ) throws TokenException
    {
        session.sendObjectToClient( reply );
    }

    protected void doClientData( NodeTCPSession session, Chunk chunk ) throws TokenException
    {
        session.sendObjectToServer( chunk );
    }

    protected void doClientDataEnd( NodeTCPSession session ) throws TokenException { }

    protected void doServerData( NodeTCPSession session, Chunk chunk ) throws TokenException
    {
        session.sendObjectToClient( chunk );
    }

    protected void doServerDataEnd( NodeTCPSession session ) throws TokenException { }

    // AbstractTokenHandler methods -------------------------------------------

    public void handleClientToken( NodeTCPSession session, Token token ) throws TokenException
    {
        Fitting clientFitting = session.pipelineConnector().getInputFitting();

        if (Fitting.FTP_CTL_TOKENS == clientFitting) {
            doCommand( session, (FtpCommand)token );
            return;
        } else if (Fitting.FTP_DATA_TOKENS == clientFitting) {
            if (token instanceof EndMarker) {
                session.sendObjectToServer( EndMarker.MARKER );
                return;
            } else if (token instanceof Chunk) {
                doClientData( session, (Chunk)token );
                return;
            } else {
                throw new TokenException("bad token: " + token);
            }
        } else {
            throw new IllegalStateException("bad fitting: " + clientFitting);
        }
    }

    public void handleServerToken( NodeTCPSession session, Token token ) throws TokenException
    {
        Fitting serverFitting = session.pipelineConnector().getOutputFitting();

        if (Fitting.FTP_CTL_TOKENS == serverFitting) {
            doReply( session, (FtpReply)token );
            return;
        } else if (Fitting.FTP_DATA_TOKENS == serverFitting) {
            if (token instanceof EndMarker) {
                session.sendObjectToClient( EndMarker.MARKER );
                return;
            } else if (token instanceof Chunk) {
                doServerData( session, (Chunk)token );
                return;
            } else {
                throw new TokenException("bad token: " + token);
            }
        } else {
            throw new IllegalStateException("bad fitting: " + serverFitting);
        }
    }

    @Override
    public void handleClientFin( NodeTCPSession session ) throws TokenException
    {
        doClientDataEnd( session );
    }

    @Override
    public void handleServerFin( NodeTCPSession session ) throws TokenException
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
