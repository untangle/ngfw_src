/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.token;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.AbstractEventHandler;
import com.metavize.mvvm.tapi.MPipeException;
import com.metavize.mvvm.tapi.Pipeline;
import com.metavize.mvvm.tapi.PipelineFoundry;
import com.metavize.mvvm.tapi.Session;
import com.metavize.mvvm.tapi.TCPNewSessionRequest;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tapi.event.IPDataResult;
import com.metavize.mvvm.tapi.event.IPSessionEvent;
import com.metavize.mvvm.tapi.event.TCPChunkEvent;
import com.metavize.mvvm.tapi.event.TCPChunkResult;
import com.metavize.mvvm.tapi.event.TCPNewSessionRequestEvent;
import com.metavize.mvvm.tapi.event.TCPSessionEvent;
import com.metavize.mvvm.tapi.event.UDPNewSessionRequestEvent;
import com.metavize.mvvm.tapi.event.UDPPacketEvent;
import com.metavize.mvvm.tapi.event.UDPSessionEvent;
import org.apache.log4j.Logger;

public class TokenAdaptor extends AbstractEventHandler
{
    private static final ByteBuffer[] BYTE_BUFFER_PROTO = new ByteBuffer[0];

    private final TokenHandlerFactory handlerFactory;
    private final Map handlers = new ConcurrentHashMap();

    private final PipelineFoundry pipeFoundry = MvvmContextFactory.context()
        .pipelineFoundry();
    private final Logger logger = Logger.getLogger(TokenAdaptor.class);

    public TokenAdaptor(TokenHandlerFactory thf)
    {
        this.handlerFactory = thf;
    }

    @Override
    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent e)
        throws MPipeException
    {
        TCPNewSessionRequest sr = e.sessionRequest();
    }

    @Override
    public void handleTCPNewSession(TCPSessionEvent e)
        throws MPipeException
    {
        TCPSession s = e.session();
        TokenHandler h = handlerFactory.tokenHandler(s);
        Pipeline pipeline = pipeFoundry.getPipeline(s.id());
        addHandler(s, h, pipeline);
        logger.debug("new session, s: " + s + " h: " + h);

        s.clientReadLimit(8); /* XXX + magic */
        s.clientLineBuffering(false);
        s.serverReadLimit(8); /* XXX + magic */
        s.serverLineBuffering(false);
    }

    @Override
    public IPDataResult handleTCPServerChunk(TCPChunkEvent e)
        throws MPipeException
    {
        HandlerDesc handlerDesc = getHandlerDesc(e.session());
        return handleToken(handlerDesc, e, true);
    }

    @Override
    public IPDataResult handleTCPClientChunk(TCPChunkEvent e)
        throws MPipeException
    {
        HandlerDesc handlerDesc = getHandlerDesc(e.session());
        return handleToken(handlerDesc, e, false);
    }

    @Override
    public void handleTCPClientFIN(TCPSessionEvent e)
        throws MPipeException
    {
        TCPSession session = (TCPSession)e.session();
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
        throws MPipeException
    {
        TCPSession session = (TCPSession)e.session();
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
    public void handleTCPFinalized(TCPSessionEvent e) throws MPipeException
    {
        super.handleTCPFinalized(e);
        removeHandler(e.session());
    }

    // UDP events -------------------------------------------------------------

    @Override
    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent e)
        throws MPipeException
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPNewSession(UDPSessionEvent e) throws MPipeException
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPClientPacket(UDPPacketEvent e)
        throws MPipeException
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPServerPacket(UDPPacketEvent e)
        throws MPipeException
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPClientExpired(UDPSessionEvent e) throws MPipeException
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPServerExpired(UDPSessionEvent e) throws MPipeException
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    @Override
    public void handleUDPFinalized(UDPSessionEvent e) throws MPipeException
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

    private void addHandler(Session session, TokenHandler handler,
                            Pipeline pipeline)
    {
        handlers.put(session, new HandlerDesc(handler, pipeline));
    }

    private HandlerDesc getHandlerDesc(Session session)
    {
        HandlerDesc handlerDesc = (HandlerDesc)handlers.get(session);
        return handlerDesc;
    }

    private TokenHandler getHandler(Session session)
    {
        HandlerDesc handlerDesc = (HandlerDesc)handlers.get(session);
        return handlerDesc.handler;
    }

    private Pipeline getPipeline(Session session)
    {
        HandlerDesc handlerDesc = (HandlerDesc)handlers.get(session);
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

        if (b.remaining() < 8) { /* XXX remember + magic */
            // read limit 8
            b.compact();
            b.limit(8);
            logger.debug("returning buffer, for more: " + b);
            return new TCPChunkResult(BYTE_BUFFER_PROTO, BYTE_BUFFER_PROTO, b);
        }

        Long key = new Long(b.getLong());
        b.position(0).limit(8); /* XXX + magic */

        Token token = (Token)pipeline.detach(key);
        logger.debug("RETRIEVED object " + token + " with key: " + key);

        TCPSession session = e.session();

        TokenResult tr;
        try {
            if (s2c) {
                tr = handler.handleServerToken(token);
            } else {
                tr = handler.handleClientToken(token);
            }
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
                session.beginClientStream(tr.s2cStreamer());
            } else {
                logger.debug("beginning server stream");
                session.beginServerStream(tr.c2sStreamer());
            }
            // just means nothing extra to send before beginning stream.
            return IPDataResult.DO_NOT_PASS;
        } else {
            logger.debug("processing s2c tokens");
            ByteBuffer[] cr = processResults(tr.s2cTokens(), pipeline);
            logger.debug("processing c2s");
            ByteBuffer[] sr = processResults(tr.c2sTokens(), pipeline);

            logger.debug("returning results, readBuffer: " + b);
            for (int i = 0; i < cr.length; i++) {
                logger.debug("  to client: " + cr[i]);
            }
            for (int i = 0; i < sr.length; i++) {
                logger.debug("  to server: " + sr[i]);
            }
            return new TCPChunkResult(cr, sr, b);
        }
    }

    private ByteBuffer[] processResults(Token[] results, Pipeline pipeline)
    {
        // XXX factor out token writing
        // XXX add magic:
        ByteBuffer bb = ByteBuffer.allocate(8 * results.length);

        for (int i = 0; i < results.length; i++) {
            if (null == results[i]) { continue; }

            Long key = pipeline.attach(results[i]);
            logger.debug("SAVED object " + results[i] + " with key: " + key);

            bb.putLong(key);
        }
        bb.flip();

        return new ByteBuffer[] { bb };
    }
}

