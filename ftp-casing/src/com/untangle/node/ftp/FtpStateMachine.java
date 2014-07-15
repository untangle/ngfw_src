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
import com.untangle.node.token.TokenResult;
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

    protected TokenResult doCommand( NodeTCPSession session, FtpCommand command ) throws TokenException
    {
        return new TokenResult(null, new Token[] { command });
    }

    protected TokenResult doReply( NodeTCPSession session, FtpReply reply ) throws TokenException
    {
        return new TokenResult(new Token[] { reply }, null);
    }

    protected TokenResult doClientData( NodeTCPSession session, Chunk c ) throws TokenException
    {
        return new TokenResult(null, new Token[] { c });
    }

    protected void doClientDataEnd( NodeTCPSession session ) throws TokenException { }

    protected TokenResult doServerData( NodeTCPSession session, Chunk c ) throws TokenException
    {
        return new TokenResult(new Token[] { c }, null);
    }

    protected void doServerDataEnd( NodeTCPSession session ) throws TokenException { }

    // AbstractTokenHandler methods -------------------------------------------

    public TokenResult handleClientToken( NodeTCPSession session, Token token ) throws TokenException
    {
        Fitting clientFitting = session.pipelineConnector().getInputFitting();

        if (Fitting.FTP_CTL_TOKENS == clientFitting) {
            return doCommand( session, (FtpCommand)token );
        } else if (Fitting.FTP_DATA_TOKENS == clientFitting) {
            if (token instanceof EndMarker) {
                return new TokenResult(null, new Token[] { EndMarker.MARKER });
            } else if (token instanceof Chunk) {
                return doClientData( session, (Chunk)token );
            } else {
                throw new TokenException("bad token: " + token);
            }
        } else {
            throw new IllegalStateException("bad fitting: " + clientFitting);
        }
    }

    public TokenResult handleServerToken( NodeTCPSession session, Token token ) throws TokenException
    {
        Fitting serverFitting = session.pipelineConnector().getOutputFitting();

        if (Fitting.FTP_CTL_TOKENS == serverFitting) {
            return doReply( session, (FtpReply)token );
        } else if (Fitting.FTP_DATA_TOKENS == serverFitting) {
            if (token instanceof EndMarker) {
                return new TokenResult(new Token[] { EndMarker.MARKER }, null);
            } else if (token instanceof Chunk) {
                return doServerData( session, (Chunk)token );
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
