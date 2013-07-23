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
import com.untangle.uvm.vnet.Pipeline;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * State machine for FTP traffic.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class FtpStateMachine extends AbstractTokenHandler
{
    private final Fitting clientFitting;
    private final Fitting serverFitting;

    /**
     * Used to obtain the control session that opened the data session on the
     * given port.
     */
    private static final Map<InetSocketAddress, Long> ctlSessionIdByDataSocket = new ConcurrentHashMap<InetSocketAddress, Long>();
    
    // constructors -----------------------------------------------------------

    protected FtpStateMachine(NodeTCPSession session)
    {
        super(session);

        Pipeline p = getPipeline();
        clientFitting = p.getClientFitting(session.pipelineConnector());
        serverFitting = p.getServerFitting(session.pipelineConnector());
    }

    // protected methods ------------------------------------------------------

    protected TokenResult doCommand(FtpCommand command) throws TokenException
    {
        return new TokenResult(null, new Token[] { command });
    }

    protected TokenResult doReply(FtpReply reply) throws TokenException
    {
        return new TokenResult(new Token[] { reply }, null);
    }

    protected TokenResult doClientData(Chunk c) throws TokenException
    {
        return new TokenResult(null, new Token[] { c });
    }

    protected void doClientDataEnd() throws TokenException { }

    protected TokenResult doServerData(Chunk c) throws TokenException
    {
        return new TokenResult(new Token[] { c }, null);
    }

    protected void doServerDataEnd() throws TokenException { }

    // AbstractTokenHandler methods -------------------------------------------

    public TokenResult handleClientToken(Token token) throws TokenException
    {
        if (Fitting.FTP_CTL_TOKENS == clientFitting) {
            return doCommand((FtpCommand)token);
        } else if (Fitting.FTP_DATA_TOKENS == clientFitting) {
            if (token instanceof EndMarker) {
                return new TokenResult(null, new Token[] { EndMarker.MARKER });
            } else if (token instanceof Chunk) {
                return doClientData((Chunk)token);
            } else {
                throw new TokenException("bad token: " + token);
            }
        } else {
            throw new IllegalStateException("bad fitting: " + clientFitting);
        }
    }

    public TokenResult handleServerToken(Token token) throws TokenException
    {
        if (Fitting.FTP_CTL_TOKENS == serverFitting) {
            return doReply((FtpReply)token);
        } else if (Fitting.FTP_DATA_TOKENS == serverFitting) {
            if (token instanceof EndMarker) {
                return new TokenResult(new Token[] { EndMarker.MARKER }, null);
            } else if (token instanceof Chunk) {
                return doServerData((Chunk)token);
            } else {
                throw new TokenException("bad token: " + token);
            }
        } else {
            throw new IllegalStateException("bad fitting: " + serverFitting);
        }
    }

    @Override
    public void handleClientFin() throws TokenException
    {
        doClientDataEnd();
    }

    @Override
    public void handleServerFin() throws TokenException
    {
        doServerDataEnd();
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
    public static void removeDataSockets(long ctlSessionId){
        Set<InetSocketAddress> set = new HashSet<InetSocketAddress>(ctlSessionIdByDataSocket.keySet());
        for (InetSocketAddress dataSocket : set){
            if (ctlSessionIdByDataSocket.get(dataSocket).longValue() == ctlSessionId)
                ctlSessionIdByDataSocket.remove(dataSocket);
        }
    }
}
