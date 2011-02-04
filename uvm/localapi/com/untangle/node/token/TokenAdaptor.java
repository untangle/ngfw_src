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

package com.untangle.node.token;

import static com.untangle.node.token.CasingAdaptor.TOKEN_SIZE;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.message.BlingBlinger;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.Pipeline;
import com.untangle.uvm.vnet.PipelineFoundry;
import com.untangle.uvm.vnet.Session;
import com.untangle.uvm.vnet.TCPSession;
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
 * Adapts a Token session's underlying byte-stream a
 * <code>TokenHandler</code>.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class TokenAdaptor extends AbstractEventHandler
{
    private static final ByteBuffer[] BYTE_BUFFER_PROTO = new ByteBuffer[0];

    private final TokenHandlerFactory handlerFactory;
    private final Map<Session,HandlerDesc> handlers = new ConcurrentHashMap<Session,HandlerDesc>();

    private final PipelineFoundry pipeFoundry = LocalUvmContextFactory.context().pipelineFoundry();

    private final BlingBlinger s2nBytes;
    private final BlingBlinger c2nBytes;
    private final BlingBlinger n2sBytes;
    private final BlingBlinger n2cBytes;

    private final Logger logger = Logger.getLogger(TokenAdaptor.class);

    public TokenAdaptor(Node node, TokenHandlerFactory thf)
    {
        super(node);
        this.handlerFactory = thf;

        MessageManager lmm = LocalUvmContextFactory.context()
            .messageManager();
        Counters c = lmm.getCounters(node.getNodeId());
        s2nBytes = c.getBlingBlinger("s2nBytes");
        c2nBytes = c.getBlingBlinger("c2nBytes");
        n2sBytes = c.getBlingBlinger("n2sBytes");
        n2cBytes = c.getBlingBlinger("n2cBytes");
    }

    @Override
    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent e)
        
    {
        handlerFactory.handleNewSessionRequest(e.sessionRequest());
    }

    @Override
    public void handleTCPNewSession(TCPSessionEvent e)
        
    {
        TCPSession s = e.session();
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
        TCPSession session = e.session();
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
        TCPSession session = e.session();
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
        TCPSession session = e.session();
        HandlerDesc handlerDesc = getHandlerDesc(session);

        try {
            handlerDesc.handler.handleFinalized();
        } catch (TokenException exn) {
            logger.warn("resetting connection", exn);
            session.resetClient();
            session.resetServer();
        }

        super.handleTCPFinalized(e);
        removeHandler(e.session());
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

    private void addHandler(Session session, TokenHandler handler, Pipeline pipeline)
    {
        handlers.put(session, new HandlerDesc(handler, pipeline));
    }

    private HandlerDesc getHandlerDesc(Session session)
    {
        HandlerDesc handlerDesc = handlers.get(session);
        return handlerDesc;
    }

    private TokenHandler getHandler(Session session)
    {
        HandlerDesc handlerDesc = handlers.get(session);
        return handlerDesc.handler;
    }

    @SuppressWarnings("unused")
	private Pipeline getPipeline(Session session)
    {
        HandlerDesc handlerDesc = handlers.get(session);
        return handlerDesc.pipeline;
    }

    private void removeHandler(Session session)
    {
        handlers.remove(session);
    }

    // private methods --------------------------------------------------------

    private IPDataResult handleToken(HandlerDesc handlerDesc, TCPChunkEvent e,
                                     boolean s2c)
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

        TCPSession session = e.session();

        try {
            if (s2c) {
                s2nBytes.increment(token.getEstimatedSize() - TOKEN_SIZE);
            } else {
                c2nBytes.increment(token.getEstimatedSize() - TOKEN_SIZE);
            }
        } catch (Exception exn) {
            logger.warn("could not get estimated size", exn);
        }

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
                TokenStreamerWrapper wrapper
                    = new TokenStreamerWrapper(tokSt, session, true);
                TCPStreamer ts = new TokenStreamerAdaptor(pipeline, wrapper);
                session.beginClientStream(ts);
            } else {
                logger.debug("beginning server stream");
                TokenStreamer tokSt = tr.c2sStreamer();
                TokenStreamerWrapper wrapper
                    = new TokenStreamerWrapper(tokSt, session, false);
                TCPStreamer ts = new TokenStreamerAdaptor(pipeline, wrapper);
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

    public TokenResult doToken(TCPSession session, boolean s2c,
                               Pipeline pipeline, TokenHandler handler,
                               Token token)
        throws TokenException
    {
        if (token instanceof Release) {
            Release release = (Release)token;

            TokenResult utr = handler.releaseFlush();

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

    private ByteBuffer[] processResults(Token[] results, Pipeline pipeline,
                                        Session session, boolean s2c)
    {
        // XXX factor out token writing
        ByteBuffer bb = ByteBuffer.allocate(TOKEN_SIZE * results.length);

        for (Token tok : results) {
            if (null == tok) { continue; }

            try {
                if (s2c) {
                    n2cBytes.increment(tok.getEstimatedSize() - TOKEN_SIZE);
                } else {
                    n2sBytes.increment(tok.getEstimatedSize() - TOKEN_SIZE);
                }
            } catch (Exception exn) {
                logger.warn("could not estimate size", exn);
            }

            Long key = pipeline.attach(tok);
            if (logger.isDebugEnabled())
                logger.debug("SAVED object " + tok + " with key: " + key);

            bb.putLong(key);
        }
        bb.flip();

        return 0 == bb.remaining() ? null : new ByteBuffer[] { bb };
    }
}

