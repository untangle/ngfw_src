/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: TokenAdaptor.java,v 1.27 2005/01/30 09:20:30 amread Exp $
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

    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent e)
        throws MPipeException
    {
        TCPNewSessionRequest sr = e.sessionRequest();
    }

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

    public IPDataResult handleTCPServerChunk(TCPChunkEvent e)
        throws MPipeException
    {
        HandlerDesc handlerDesc = getHandlerDesc(e.session());
        return handleToken(handlerDesc, e, true);
    }

    public IPDataResult handleTCPClientChunk(TCPChunkEvent e)
        throws MPipeException
    {
        HandlerDesc handlerDesc = getHandlerDesc(e.session());
        return handleToken(handlerDesc, e, false);
    }

    public void handleTCPClientFIN(TCPSessionEvent e)
        throws MPipeException
    {
        super.handleTCPClientFIN(e);
    }

    public void handleTCPServerFIN(TCPSessionEvent e)
        throws MPipeException
    {
        super.handleTCPServerFIN(e);
    }

    public void handleTCPFinalized(TCPSessionEvent e) throws MPipeException
    {
        super.handleTCPFinalized(e);
        removeHandler(e.session());
    }

    // UDP events -------------------------------------------------------------

    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent e)
        throws MPipeException
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    public void handleUDPNewSession(UDPSessionEvent e) throws MPipeException
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    public IPDataResult handleUDPClientPacket(UDPPacketEvent e)
        throws MPipeException
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    public IPDataResult handleUDPServerPacket(UDPPacketEvent e)
        throws MPipeException
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    public void handleUDPClientExpired(UDPSessionEvent e) throws MPipeException
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    public void handleUDPServerExpired(UDPSessionEvent e) throws MPipeException
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    public void handleUDPFinalized(UDPSessionEvent e) throws MPipeException
    {
        throw new UnsupportedOperationException("UDP not supported");
    }

    public void handleTimer(IPSessionEvent e)
    {
        TokenHandler th = getHandler(e.ipsession());
        th.handleTimer(new TokenEvent((TCPSessionEvent)e, null));
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

        TokenResult tr;
        if (s2c) {
            tr = handler.handleServerToken(new TokenEvent(e, token));
        } else {
            tr = handler.handleClientToken(new TokenEvent(e, token));
        }

        TCPSession s = e.session();

        // XXX ugly:
        if (tr.isStreamer()) {
            if (tr.s2cStreamer() != null) {
                logger.debug("beginning client stream");
                s.beginClientStream(tr.s2cStreamer());
            } else {
                logger.debug("beginning server stream");
                s.beginServerStream(tr.c2sStreamer());
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

