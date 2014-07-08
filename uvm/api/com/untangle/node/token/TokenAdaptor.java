/**
 * $Id$
 */
package com.untangle.node.token;

import static com.untangle.node.token.CasingAdaptor.TOKEN_SIZE;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.NodeUDPSession;
import com.untangle.uvm.vnet.UDPNewSessionRequest;
import com.untangle.uvm.vnet.IPPacketHeader;
import com.untangle.uvm.vnet.event.TCPStreamer;

/**
 * Adapts a Token session's underlying byte-stream a <code>TokenHandler</code>.
 */
public class TokenAdaptor extends AbstractEventHandler
{
    private static final ByteBuffer[] BYTE_BUFFER_PROTO = new ByteBuffer[0];

    private final TokenHandlerFactory handlerFactory;

    private final Logger logger = Logger.getLogger(TokenAdaptor.class);

    public TokenAdaptor(Node node, TokenHandlerFactory thf)
    {
        super(node);
        this.handlerFactory = thf;
    }

    @Override
    public void handleTCPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        handlerFactory.handleNewSessionRequest( sessionRequest );
    }

    @Override
    public void handleTCPNewSession( NodeTCPSession session )
    {
        TokenHandler handler = handlerFactory.tokenHandler( session );
        session.attach( handler );
        
        session.clientReadBufferSize(TOKEN_SIZE);
        session.clientLineBuffering(false);
        session.serverReadBufferSize(TOKEN_SIZE);
        session.serverLineBuffering(false);
        // (read limits are automatically set to the buffer size)
    }

    @Override
    public void handleTCPServerChunk( NodeTCPSession session, ByteBuffer data )
    {
        TokenHandler handler = (TokenHandler) session.attachment();
        handleToken( handler, session, data, true );
        return;
    }

    @Override
    public void handleTCPClientChunk( NodeTCPSession session, ByteBuffer data )
    {
        TokenHandler handler = (TokenHandler) session.attachment();
        handleToken( handler, session, data, false );
        return;
    }

    @Override
    public void handleTCPClientFIN( NodeTCPSession session )
    {
        TokenHandler handler = (TokenHandler) session.attachment();

        try {
            handler.handleClientFin();
        } catch (TokenException exn) {
            logger.warn("resetting connection", exn);
            session.resetClient();
            session.resetServer();
        }
    }

    @Override
    public void handleTCPServerFIN( NodeTCPSession session )
    {
        TokenHandler handler = (TokenHandler) session.attachment();

        try {
            handler.handleServerFin();
        } catch (TokenException exn) {
            logger.warn("resetting connection", exn);
            session.resetClient();
            session.resetServer();
        }
    }

    @Override
    public void handleTCPFinalized( NodeTCPSession session ) 
    {
        finalize( session );
        
        super.handleTCPFinalized( session );
    }

    private void finalize( NodeTCPSession sess )
    {
        TokenHandler handler = (TokenHandler) sess.attachment();

        try {
            handler.handleFinalized();
        } catch ( Exception exn ) {
            logger.warn("Exception. resetting connection", exn);
            sess.resetClient();
            sess.resetServer();
        }

        sess.attach( null ); // remove tokenHandler reference
    }
    // UDP events -------------------------------------------------------------

    @Override
    public void handleUDPNewSessionRequest( UDPNewSessionRequest sessionRequest )
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPNewSession( NodeUDPSession session ) 
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPClientPacket( NodeUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPServerPacket(  NodeUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPClientExpired( NodeUDPSession session ) 
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPServerExpired( NodeUDPSession session ) 
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPFinalized( NodeUDPSession session ) 
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleTimer( NodeSession sess )
    {
        TokenHandler handler = (TokenHandler) sess.attachment();

        try {
            handler.handleTimer();
        } catch (TokenException exn) {
            logger.warn("exception in timer, no action taken", exn);
        }
    }

    // private methods --------------------------------------------------------

    private void handleToken(TokenHandler handler, NodeTCPSession session, ByteBuffer data, boolean s2c)
    {
        ByteBuffer b = data;

        if (b.remaining() < TOKEN_SIZE) {
            // read limit to token size
            b.compact();
            b.limit(TOKEN_SIZE);
            logger.debug("returning buffer, for more: " + b);

            if ( s2c )
                session.setServerBuffer( b );
            else
                session.setClientBuffer( b );
            return;
        }

        Long key = new Long(b.getLong());

        Token token = (Token) session.globalAttachment( key );
        session.globalAttach( key, null ); // remove attachment
        
        if (logger.isDebugEnabled())
            logger.debug("RETRIEVED object " + token + " with key: " + key);

        TokenResult tr;
        try {
            tr = doToken(session, s2c, handler, token);
        } catch (TokenException exn) {
            logger.warn("resetting connection", exn);
            session.resetClient();
            session.resetServer();
            return;
        }

        // XXX ugly:
        if (tr.isStreamer()) {
            if (tr.s2cStreamer() != null) {
                logger.debug("beginning client stream");
                TokenStreamer tokSt = tr.s2cStreamer();
                TCPStreamer ts = new TokenStreamerAdaptor( tokSt, session );
                session.beginClientStream(ts);
            } else {
                logger.debug("beginning server stream");
                TokenStreamer tokSt = tr.c2sStreamer();
                TCPStreamer ts = new TokenStreamerAdaptor( tokSt, session );
                session.beginServerStream(ts);
            }
            // just means nothing extra to send before beginning stream.
            return;
        } else {
            logger.debug("processing s2c tokens");
            ByteBuffer[] cr = processResults(tr.s2cTokens(), session, true);
            logger.debug("processing c2s tokens");
            ByteBuffer[] sr = processResults(tr.c2sTokens(), session, false);

            if (logger.isDebugEnabled()) {
                logger.debug("returning results: ");
                for (int i = 0; null != cr && i < cr.length; i++) {
                    logger.debug("  to client: " + cr[i]);
                }
                for (int i = 0; null != sr && i < sr.length; i++) {
                    logger.debug("  to server: " + sr[i]);
                }
            }

            session.sendDataToClient( cr );
            session.sendDataToServer( sr );
            return;
        }
    }

    public TokenResult doToken(NodeTCPSession session, boolean s2c, TokenHandler handler, Token token)
        throws TokenException
    {
        if (token instanceof Release) {
            Release release = (Release)token;

            TokenResult utr = handler.releaseFlush();

            finalize( session );
            session.release();

            if (utr.isStreamer()) {
                if (s2c) {
                    TokenStreamer cStm = utr.c2sStreamer();
                    TokenStreamer sStm = new ReleaseTokenStreamer
                        (utr.s2cStreamer(), release);

                    return new TokenResult(sStm, cStm);
                } else {
                    TokenStreamer cStm = new ReleaseTokenStreamer
                        (utr.c2sStreamer(), release);
                    TokenStreamer sStm = utr.s2cStreamer();

                    return new TokenResult(sStm, cStm);
                }
            } else {
                if (s2c) {
                    Token[] cTok = utr.c2sTokens();

                    Token[] sTokOrig = utr.s2cTokens();
                    Token[] sTok = new Token[sTokOrig.length + 1];
                    System.arraycopy(sTokOrig, 0, sTok, 0, sTokOrig.length);
                    sTok[sTok.length - 1] = release;

                    return new TokenResult(sTok, cTok);
                } else {
                    Token[] cTokOrig = utr.c2sTokens();
                    Token[] cTok = new Token[cTokOrig.length + 1];
                    System.arraycopy(cTokOrig, 0, cTok, 0, cTokOrig.length);
                    cTok[cTok.length - 1] = release;
                    Token[] sTok = utr.s2cTokens();
                    return new TokenResult(sTok, cTok);
                }
            }
        } else {
            if (s2c) {
                return handler.handleServerToken(token);
            } else {
                return handler.handleClientToken(token);
            }
        }
    }

    private ByteBuffer[] processResults(Token[] results, NodeSession session, boolean s2c)
    {
        // XXX factor out token writing
        ByteBuffer bb = ByteBuffer.allocate(TOKEN_SIZE * results.length);

        for (Token tok : results) {
            if (null == tok) { continue; }

            Long key = session.getUniqueGlobalAttachmentKey();
            session.globalAttach( key, tok );
            
            if (logger.isDebugEnabled())
                logger.debug("SAVED object " + tok + " with key: " + key);

            bb.putLong(key);
        }
        bb.flip();

        return 0 == bb.remaining() ? null : new ByteBuffer[] { bb };
    }
}

