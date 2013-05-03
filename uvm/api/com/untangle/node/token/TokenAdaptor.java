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
import com.untangle.uvm.vnet.Pipeline;
import com.untangle.uvm.vnet.PipelineFoundry;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.event.IPDataResult;
import com.untangle.uvm.vnet.event.IPSessionEvent;
import com.untangle.uvm.vnet.event.TCPChunkEvent;
import com.untangle.uvm.vnet.event.TCPChunkResult;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.TCPStreamer;
import com.untangle.uvm.vnet.event.UDPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.UDPPacketEvent;
import com.untangle.uvm.vnet.event.UDPSessionEvent;

/**
 * Adapts a Token session's underlying byte-stream a <code>TokenHandler</code>.
 */
public class TokenAdaptor extends AbstractEventHandler
{
    private static final ByteBuffer[] BYTE_BUFFER_PROTO = new ByteBuffer[0];

    private final TokenHandlerFactory handlerFactory;
    private final Map<NodeSession,HandlerDesc> handlers = new ConcurrentHashMap<NodeSession,HandlerDesc>();

    private final PipelineFoundry pipeFoundry = UvmContextFactory.context().pipelineFoundry();

    private final Logger logger = Logger.getLogger(TokenAdaptor.class);

    public TokenAdaptor(Node node, TokenHandlerFactory thf)
    {
        super(node);
        this.handlerFactory = thf;
    }

    @Override
    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent e)
    {
        handlerFactory.handleNewSessionRequest(e.sessionRequest());
    }

    @Override
    public void handleTCPNewSession(TCPSessionEvent e)
    {
        NodeTCPSession s = e.session();
        TokenHandler h = handlerFactory.tokenHandler(s);
        Pipeline pipeline = pipeFoundry.getPipeline(s.id());
        addHandler(s, h, pipeline);
        logger.debug("new session, s: " + s + " h: " + h);

        s.clientReadBufferSize(TOKEN_SIZE);
        s.clientLineBuffering(false);
        s.serverReadBufferSize(TOKEN_SIZE);
        s.serverLineBuffering(false);
        // (read limits are automatically set to the buffer size)
    }

    @Override
    public IPDataResult handleTCPServerChunk(TCPChunkEvent e)
    {
        HandlerDesc handlerDesc = getHandlerDesc(e.session());
        return handleToken(handlerDesc, e, true);
    }

    @Override
    public IPDataResult handleTCPClientChunk(TCPChunkEvent e)
    {
        HandlerDesc handlerDesc = getHandlerDesc(e.session());
        return handleToken(handlerDesc, e, false);
    }

    @Override
    public void handleTCPClientFIN(TCPSessionEvent e)
    {
        NodeTCPSession session = e.session();
        HandlerDesc handlerDesc = getHandlerDesc(session);

        try {
            handlerDesc.handler.handleClientFin();
        } catch (TokenException exn) {
            logger.warn("resetting connection", exn);
            session.resetClient();
            session.resetServer();
        }
    }

    @Override
    public void handleTCPServerFIN(TCPSessionEvent e)
    {
        NodeTCPSession session = e.session();
        HandlerDesc handlerDesc = getHandlerDesc(session);

        try {
            handlerDesc.handler.handleServerFin();
        } catch (TokenException exn) {
            logger.warn("resetting connection", exn);
            session.resetClient();
            session.resetServer();
        }
    }

    @Override
    public void handleTCPFinalized(TCPSessionEvent e) 
    {
        NodeTCPSession sess = e.session();

        finalize( sess );
        
        super.handleTCPFinalized(e);
    }

    private void finalize( NodeTCPSession sess )
    {
        HandlerDesc handlerDesc = getHandlerDesc( sess );

        try {
            handlerDesc.handler.handleFinalized();
        } catch (TokenException exn) {
            logger.warn("resetting connection", exn);
            sess.resetClient();
            sess.resetServer();
        }

        removeHandler( sess );
    }
    // UDP events -------------------------------------------------------------

    @Override
    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent e)
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPNewSession(UDPSessionEvent e) 
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPClientPacket(UDPPacketEvent e)
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPServerPacket(UDPPacketEvent e)
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPClientExpired(UDPSessionEvent e) 
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPServerExpired(UDPSessionEvent e) 
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPFinalized(UDPSessionEvent e) 
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleTimer(IPSessionEvent e)
    {
        TokenHandler th = getHandler(e.ipsession());
        try {
            th.handleTimer();
        } catch (TokenException exn) {
            logger.warn("exception in timer, no action taken", exn);
        }
    }

    // HandlerDesc utils ------------------------------------------------------

    private static class HandlerDesc
    {
        final TokenHandler handler;
        final Pipeline pipeline;

        HandlerDesc(TokenHandler handler, Pipeline pipeline)
        {
            this.handler = handler;
            this.pipeline = pipeline;
        }
    }

    private void addHandler(NodeSession session, TokenHandler handler, Pipeline pipeline)
    {
        handlers.put(session, new HandlerDesc(handler, pipeline));
    }

    private HandlerDesc getHandlerDesc(NodeSession session)
    {
        HandlerDesc handlerDesc = handlers.get(session);
        return handlerDesc;
    }

    private TokenHandler getHandler(NodeSession session)
    {
        HandlerDesc handlerDesc = handlers.get(session);
        return handlerDesc.handler;
    }

    @SuppressWarnings("unused")
	private Pipeline getPipeline(NodeSession session)
    {
        HandlerDesc handlerDesc = handlers.get(session);
        return handlerDesc.pipeline;
    }

    private void removeHandler(NodeSession session)
    {
        handlers.remove(session);
    }

    // private methods --------------------------------------------------------

    private IPDataResult handleToken(HandlerDesc handlerDesc, TCPChunkEvent e, boolean s2c)
    {
        TokenHandler handler = handlerDesc.handler;
        Pipeline pipeline = handlerDesc.pipeline;

        ByteBuffer b = e.chunk();

        if (b.remaining() < TOKEN_SIZE) {
            // read limit to token size
            b.compact();
            b.limit(TOKEN_SIZE);
            logger.debug("returning buffer, for more: " + b);
            return new TCPChunkResult(BYTE_BUFFER_PROTO, BYTE_BUFFER_PROTO, b);
        }

        Long key = new Long(b.getLong());

        Token token = (Token)pipeline.detach(key);
        if (logger.isDebugEnabled())
            logger.debug("RETRIEVED object " + token + " with key: " + key);

        NodeTCPSession session = e.session();

        TokenResult tr;
        try {
            tr = doToken(session, s2c, pipeline, handler, token);
        } catch (TokenException exn) {
            logger.warn("resetting connection", exn);
            session.resetClient();
            session.resetServer();
            return IPDataResult.DO_NOT_PASS;
        }

        // XXX ugly:
        if (tr.isStreamer()) {
            if (tr.s2cStreamer() != null) {
                logger.debug("beginning client stream");
                TokenStreamer tokSt = tr.s2cStreamer();
                TCPStreamer ts = new TokenStreamerAdaptor(pipeline, tokSt);
                session.beginClientStream(ts);
            } else {
                logger.debug("beginning server stream");
                TokenStreamer tokSt = tr.c2sStreamer();
                TCPStreamer ts = new TokenStreamerAdaptor(pipeline, tokSt);
                session.beginServerStream(ts);
            }
            // just means nothing extra to send before beginning stream.
            return IPDataResult.SEND_NOTHING;
        } else {
            logger.debug("processing s2c tokens");
            ByteBuffer[] cr = processResults(tr.s2cTokens(), pipeline, session,
                                             true);
            logger.debug("processing c2s");
            ByteBuffer[] sr = processResults(tr.c2sTokens(), pipeline, session,
                                             false);

            if (logger.isDebugEnabled()) {
                logger.debug("returning results: ");
                for (int i = 0; null != cr && i < cr.length; i++) {
                    logger.debug("  to client: " + cr[i]);
                }
                for (int i = 0; null != sr && i < sr.length; i++) {
                    logger.debug("  to server: " + sr[i]);
                }
            }

            return new TCPChunkResult(cr, sr, null);
        }
    }

    public TokenResult doToken(NodeTCPSession session, boolean s2c, Pipeline pipeline, TokenHandler handler, Token token)
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

    private ByteBuffer[] processResults(Token[] results, Pipeline pipeline, NodeSession session, boolean s2c)
    {
        // XXX factor out token writing
        ByteBuffer bb = ByteBuffer.allocate(TOKEN_SIZE * results.length);

        for (Token tok : results) {
            if (null == tok) { continue; }

            Long key = pipeline.attach(tok);
            if (logger.isDebugEnabled())
                logger.debug("SAVED object " + tok + " with key: " + key);

            bb.putLong(key);
        }
        bb.flip();

        return 0 == bb.remaining() ? null : new ByteBuffer[] { bb };
    }
}

