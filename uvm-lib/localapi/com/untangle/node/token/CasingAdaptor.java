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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.message.BlingBlinger;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.LocalMessageManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.MPipeException;
import com.untangle.uvm.vnet.Pipeline;
import com.untangle.uvm.vnet.PipelineFoundry;
import com.untangle.uvm.vnet.Session;
import com.untangle.uvm.vnet.TCPSession;
import com.untangle.uvm.vnet.event.IPDataResult;
import com.untangle.uvm.vnet.event.IPSessionEvent;
import com.untangle.uvm.vnet.event.TCPChunkEvent;
import com.untangle.uvm.vnet.event.TCPChunkResult;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.TCPStreamer;

/**
 * Adapts a Token session's underlying byte-stream a
 * <code>Casing</code>.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class CasingAdaptor extends AbstractEventHandler
{
    static final int TOKEN_SIZE = 8;

    private final CasingFactory casingFactory;
    private final boolean clientSide;

    private final Map<Session,CasingDesc> casings = new ConcurrentHashMap<Session,CasingDesc>();

    private final PipelineFoundry pipeFoundry = LocalUvmContextFactory.context().pipelineFoundry();

    private final BlingBlinger s2nBytes;
    private final BlingBlinger c2nBytes;
    private final BlingBlinger n2sBytes;
    private final BlingBlinger n2cBytes;

    private final Logger logger = Logger.getLogger(CasingAdaptor.class);

    private volatile boolean releaseParseExceptions;

    public CasingAdaptor(Node node, CasingFactory casingFactory,
                         boolean clientSide, boolean releaseParseExceptions)
    {
        super(node);
        this.casingFactory = casingFactory;
        this.clientSide = clientSide;
        this.releaseParseExceptions = releaseParseExceptions;

        LocalMessageManager lmm = LocalUvmContextFactory.context()
            .localMessageManager();
        Counters c = lmm.getCounters(node.getTid());
        s2nBytes = c.getBlingBlinger("s2nBytes");
        c2nBytes = c.getBlingBlinger("c2nBytes");
        n2sBytes = c.getBlingBlinger("n2sBytes");
        n2cBytes = c.getBlingBlinger("n2cBytes");
    }

    // accessors --------------------------------------------------------------

    public boolean getReleaseParseExceptions()
    {
        return releaseParseExceptions;
    }

    public void setReleaseParseExceptions(boolean releaseParseExceptions)
    {
        this.releaseParseExceptions = releaseParseExceptions;
    }

    // SessionEventListener methods -------------------------------------------

    @Override
    public void handleTCPNewSession(TCPSessionEvent e)
    {
        TCPSession session = e.session();

        Casing casing = casingFactory.casing(session, clientSide);
        Pipeline pipeline = pipeFoundry.getPipeline(session.id());

        if (logger.isDebugEnabled()) {
            logger.debug("new session setting: " + pipeline
                         + " for: " + session.id());
        }

        addCasing(session, casing, pipeline);

        if (clientSide) {
            session.serverReadLimit(TOKEN_SIZE);
        } else {
            session.clientReadLimit(TOKEN_SIZE);
        }
    }

    @Override
    public IPDataResult handleTCPClientChunk(TCPChunkEvent e)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling client chunk, session: " + e.session().id());
        }

        if (clientSide) {
            return parse(e, false, false);
        } else {
            return unparse(e, false);
        }
    }

    @Override
    public IPDataResult handleTCPServerChunk(TCPChunkEvent e)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling server chunk, session: " + e.session().id());
        }

        if (clientSide) {
            return unparse(e, true);
        } else {
            return parse(e, true, false);
        }
    }

    @Override
    public IPDataResult handleTCPClientDataEnd(TCPChunkEvent e)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling client chunk, session: " + e.session().id());
        }

        if (clientSide) {
            return parse(e, false, true);
        } else {
            if (e.chunk().hasRemaining()) {
                logger.warn("should not happen: unparse TCPClientDataEnd");
            }
            return null;
        }
    }

    @Override
    public IPDataResult handleTCPServerDataEnd(TCPChunkEvent e)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling server chunk, session: " + e.session().id());
        }

        if (clientSide) {
            if (e.chunk().hasRemaining()) {
                logger.warn("should not happen: unparse TCPClientDataEnd");
            }
            return null;
        } else {
            return parse(e, true, true);
        }
    }

    @Override
    public void handleTCPClientFIN(TCPSessionEvent e)
    {
        TCPStreamer tcpStream = null;

        TCPSession s = (TCPSession)e.ipsession();
        Casing c = getCasing(s);

        if (clientSide) {
            TokenStreamer tokSt = c.parser().endSession();
            if (null != tokSt) {
                tcpStream = new TokenStreamerAdaptor(getPipeline(s), tokSt);
            }
        } else {
            tcpStream = c.unparser().endSession();
        }

        if (null != tcpStream) {
            s.beginServerStream(tcpStream);
        } else {
            s.shutdownServer();
        }
    }

    @Override
    public void handleTCPServerFIN(TCPSessionEvent e)
    {
        TCPStreamer ts = null;

        TCPSession s = (TCPSession)e.ipsession();
        Casing c = getCasing(s);

        if (clientSide) {
            ts = c.unparser().endSession();
        } else {
            TokenStreamer tokSt = c.parser().endSession();
            if (null != tokSt) {
                ts = new TokenStreamerAdaptor(getPipeline(s), tokSt);
            }
        }

        if (null != ts) {
            s.beginClientStream(ts);
        } else {
            s.shutdownClient();
        }
    }

    @Override
    public void handleTCPFinalized(TCPSessionEvent e) throws MPipeException
    {
        if (logger.isDebugEnabled()) {
            logger.debug("finalizing " + e.session().id());
        }
        Casing c = getCasing(e.ipsession());
        c.parser().handleFinalized();
        c.unparser().handleFinalized();
        removeCasingDesc(e.session());
    }

    @Override
    public void handleTimer(IPSessionEvent e)
    {
        TCPSession s = (TCPSession)e.ipsession();

        Parser p = getCasing(s).parser();
        p.handleTimer();
        // XXX unparser doesnt get one, does it need it?
    }

    // CasingDesc utils -------------------------------------------------------

    private static class CasingDesc
    {
        final Casing casing;
        final Pipeline pipeline;

        CasingDesc(Casing casing, Pipeline pipeline)
        {
            this.casing = casing;
            this.pipeline = pipeline;
        }
    }

    private void addCasing(Session session, Casing casing, Pipeline pipeline)
    {
        casings.put(session, new CasingDesc(casing, pipeline));
    }

    private CasingDesc getCasingDesc(Session session)
    {
        CasingDesc casingDesc = casings.get(session);
        return casingDesc;
    }

    private Casing getCasing(Session session)
    {
        CasingDesc casingDesc = casings.get(session);
        return casingDesc.casing;
    }

    private Pipeline getPipeline(Session session)
    {
        CasingDesc casingDesc = casings.get(session);
        return casingDesc.pipeline;
    }

    private void removeCasingDesc(Session session)
    {
        casings.remove(session);
    }

    // private methods --------------------------------------------------------

    private IPDataResult unparse(TCPChunkEvent e, boolean s2c)
    {
        ByteBuffer b = e.chunk();

        assert b.remaining() <= TOKEN_SIZE;

        if (b.remaining() < TOKEN_SIZE) {
            // read limit 2
            b.compact();
            b.limit(TOKEN_SIZE);
            if (logger.isDebugEnabled()) {
                logger.debug("unparse returning buffer, for more: " + b);
            }
            return new TCPChunkResult(null, null, b);
        }

        TCPSession s = e.session();
        CasingDesc casingDesc = getCasingDesc(s);
        Casing casing = casingDesc.casing;
        Pipeline pipeline = casingDesc.pipeline;

        Long key = new Long(b.getLong());
        Token tok = (Token)pipeline.detach(key);

        try {
            if (s2c) {
                s2nBytes.increment(tok.getEstimatedSize() - TOKEN_SIZE);
            } else {
                c2nBytes.increment(tok.getEstimatedSize() - TOKEN_SIZE);
            }
        } catch (Exception exn) {
            logger.warn("could not estimated size", exn);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("RETRIEVED object: " + tok + " with key: " + key
                         + " on pipeline: " + pipeline);
        }

        b.limit(TOKEN_SIZE);

        assert !b.hasRemaining();

        UnparseResult ur;
        try {
            ur = unparseToken(s, casing, tok);
        } catch (Exception exn) { /* not just UnparseException */
            logger.error("internal error, closing connection", exn);
            if (s2c) {
                // XXX We don't have a good handle on this
                s.resetClient();
                s.resetServer();
            } else {
                // XXX We don't have a good handle on this
                s.shutdownServer();
                s.resetClient();
            }
            logger.debug("returning DO_NOT_PASS");

            return IPDataResult.DO_NOT_PASS;
        }

        if (ur.isStreamer()) {
            TCPStreamer ts = ur.getTcpStreamer();
            if (s2c) {
                s.beginClientStream(ts);
            } else {
                s.beginServerStream(ts);
            }
            return new TCPChunkResult(null, null, null);
        } else {
            if (s2c) {
                logger.debug("unparse result to client");
                ByteBuffer[] r = ur.result();
                if (logger.isDebugEnabled()) {
                    for (int i = 0; null != null && i < r.length; i++) {
                        logger.debug("  to client: " + r[i]);
                    }
                }
                return new TCPChunkResult(r, null, null);
            } else {
                logger.debug("unparse result to server");
                ByteBuffer[] r = ur.result();
                if (logger.isDebugEnabled()) {
                    for (int i = 0; null != r && i < r.length; i++) {
                        logger.debug("  to server: " + r[i]);
                    }
                }
                return new TCPChunkResult(null, r, null);
            }
        }
    }

    private UnparseResult unparseToken(TCPSession s, Casing c, Token token)
        throws UnparseException
    {
        Unparser u = c.unparser();

        if (token instanceof Release) {
            Release release = (Release)token;

            s.release();
            UnparseResult ur = u.releaseFlush();
            if (ur.isStreamer()) {
                TCPStreamer ts = new ReleaseTcpStreamer
                    (ur.getTcpStreamer(), release);
                return new UnparseResult(ts);
            } else {
                ByteBuffer[] orig = ur.result();
                ByteBuffer[] r = new ByteBuffer[orig.length + 1];
                System.arraycopy(orig, 0, r, 0, orig.length);
                r[r.length - 1] = release.getBytes();
                return new UnparseResult(r);
            }
        } else {
            return u.unparse(token);
        }
    }

    private IPDataResult parse(TCPChunkEvent e, boolean s2c, boolean last)
    {
        TCPSession s = e.session();
        CasingDesc casingDesc = getCasingDesc(s);
        Casing casing = casingDesc.casing;
        Pipeline pipeline = casingDesc.pipeline;

        ParseResult pr;
        ByteBuffer buf = e.chunk();
        ByteBuffer dup = buf.duplicate();
        Parser p = casing.parser();
        try {
            if (last) {
                pr = p.parseEnd(buf);
            } else {
                pr = p.parse(buf);
            }
        } catch (Throwable exn) {
            if (releaseParseExceptions) {
                String sessionEndpoints = "Endpoints ["
                    + " protocol: " + s.protocol()
                    + " clientIntf: " + s.clientIntf()
                    + " clientAddr: " + s.clientAddr()
                    + " clientPort: " + s.clientPort()
                    + " serverIntf: " + s.serverIntf()
                    + " serverAddr: " + s.serverAddr()
                    + " serverPort: " + s.serverPort() + "]";

                // XXX make configurable
                logger.warn("parse exception, releasing session. "
                            + sessionEndpoints , exn);
                s.release();
                pr = new ParseResult(new Release(dup));
            } else {
                s.shutdownServer();
                s.shutdownClient();
                return IPDataResult.DO_NOT_PASS;
            }
        }

        if (pr.isStreamer()) {
            TokenStreamer tokSt
                = new TokenStreamerWrapper(pr.getTokenStreamer(), s, s2c);
            TCPStreamer ts = new TokenStreamerAdaptor(pipeline, tokSt);
            if (s2c) {
                s.beginClientStream(ts);
            } else {
                s.beginServerStream(ts);
            }
            return new TCPChunkResult(null, null, pr.getReadBuffer());
        } else {
            List<Token> results = pr.getResults();

            // XXX add magic:
            ByteBuffer bb = ByteBuffer.allocate(TOKEN_SIZE * results.size());

            // XXX add magic:
            for (Token t : results) {
                try {
                    if (s2c) {
                        n2cBytes.increment(t.getEstimatedSize() - TOKEN_SIZE);
                    } else {
                        n2sBytes.increment(t.getEstimatedSize() - TOKEN_SIZE);
                    }
                } catch (Exception exn) {
                    logger.error("could not estimate size", exn);
                }

                Long key = pipeline.attach(t);
                if (logger.isDebugEnabled()) {
                    logger.debug("SAVED object: " + t + " with key: " + key
                                 + " on pipeline: " + pipeline);
                }
                bb.putLong(key);
            }
            bb.flip();

            ByteBuffer[] r = new ByteBuffer[] { bb };

            if (s2c) {
                if (logger.isDebugEnabled()) {
                    logger.debug("parse result to server, read buffer: "
                                 + pr.getReadBuffer()
                                 + "  to client: " + r[0]);
                }
                return new TCPChunkResult(r, null, pr.getReadBuffer());
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("parse result to client, read buffer: "
                                 + pr.getReadBuffer()
                                 + "  to server: " + r[0]);
                }
                return new TCPChunkResult(null, r, pr.getReadBuffer());
            }
        }
    }
}
