/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;
import java.util.List;
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
import com.untangle.uvm.vnet.event.IPSessionEvent;
import com.untangle.uvm.vnet.event.TCPChunkEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.TCPStreamer;

/**
 * Adapts a Token session's underlying byte-stream a <code>Casing</code>.
 */
public class CasingAdaptor extends CasingBase
{
    static final int TOKEN_SIZE = 8;

    public CasingAdaptor(Node node, CasingFactory casingFactory, boolean clientSide, boolean releaseParseExceptions)
    {
        super(node,casingFactory,clientSide,releaseParseExceptions);
    }

    // SessionEventListener methods -------------------------------------------

    @Override
    public void handleTCPNewSession(TCPSessionEvent e)
    {
        NodeTCPSession session = e.session();

        Casing casing = casingFactory.casing( session, clientSide );
        Pipeline pipeline = pipeFoundry.getPipeline( session.id() );

        if (logger.isDebugEnabled()) {
            logger.debug("new session setting: " + pipeline + " for: " + session.id());
        }

        // addCasing( session, casing, pipeline );

        session.attach( new Attachment(casing, pipeline) );
        
        if (clientSide) {
            session.serverReadLimit( TOKEN_SIZE );
        } else {
            session.clientReadLimit( TOKEN_SIZE );
        }
    }

    @Override
    public void handleTCPClientChunk(TCPChunkEvent e)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling client chunk, session: " + e.session().id());
        }

        if (clientSide) {
            parse(e, false, false);
            return;
        } else {
            unparse(e, false);
            return;
        }
    }

    @Override
    public void handleTCPServerChunk(TCPChunkEvent e)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling server chunk, session: " + e.session().id());
        }

        if (clientSide) {
            unparse(e, true);
            return;
        } else {
            parse(e, true, false);
            return;
        }
    }

    @Override
    public void handleTCPClientDataEnd(TCPChunkEvent e)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling client chunk, session: " + e.session().id());
        }

        if (clientSide) {
            parse(e, false, true);
            return;
        } else {
            if (e.chunk().hasRemaining()) {
                logger.warn("should not happen: unparse TCPClientDataEnd");
            }
            return;
        }
    }

    @Override
    public void handleTCPServerDataEnd(TCPChunkEvent e)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling server chunk, session: " + e.session().id());
        }

        if (clientSide) {
            if (e.chunk().hasRemaining()) {
                logger.warn("should not happen: unparse TCPClientDataEnd");
            }
            return;
        } else {
            parse(e, true, true);
            return;
        }
    }

    @Override
    public void handleTCPClientFIN(TCPSessionEvent e)
    {
        TCPStreamer tcpStream = null;

        NodeTCPSession s = (NodeTCPSession)e.ipsession();
        Casing casing = ((Attachment)e.ipsession().attachment()).casing;
        Pipeline pipeline = ((Attachment)e.ipsession().attachment()).pipeline;

        if (clientSide) {
            TokenStreamer tokSt = casing.parser().endSession();
            if (null != tokSt) {
                tcpStream = new TokenStreamerAdaptor( pipeline, tokSt );
            }
        } else {
            tcpStream = casing.unparser().endSession();
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

        NodeTCPSession s = (NodeTCPSession)e.ipsession();
        Casing casing = ((Attachment)e.ipsession().attachment()).casing;
        Pipeline pipeline = ((Attachment)e.ipsession().attachment()).pipeline;

        if (clientSide) {
            ts = casing.unparser().endSession();
        } else {
            TokenStreamer tokSt = casing.parser().endSession();
            if (null != tokSt) {
                ts = new TokenStreamerAdaptor( pipeline, tokSt );
            }
        }

        if (null != ts) {
            s.beginClientStream(ts);
        } else {
            s.shutdownClient();
        }
    }

    @Override
    public void handleTCPFinalized(TCPSessionEvent e) 
    {
        if (logger.isDebugEnabled()) {
            logger.debug("finalizing " + e.session().id());
        }
        finalize( e.ipsession() );
    }

    @Override
    public void handleTimer(IPSessionEvent e)
    {
        NodeTCPSession s = (NodeTCPSession)e.ipsession();
        Casing casing = ((Attachment)e.ipsession().attachment()).casing;

        Parser p = casing.parser();
        p.handleTimer();
        // XXX unparser doesnt get one, does it need it?
    }

    // private methods --------------------------------------------------------

    private void unparse(TCPChunkEvent e, boolean s2c)
    {
        ByteBuffer b = e.chunk();
        NodeTCPSession session = e.session();

        assert b.remaining() <= TOKEN_SIZE;

        if (b.remaining() < TOKEN_SIZE) {
            // read limit 2
            b.compact();
            b.limit(TOKEN_SIZE);
            if (logger.isDebugEnabled()) {
                logger.debug("unparse returning buffer, for more: " + b);
            }
            if ( s2c )
                session.setServerBuffer( b );
            else
                session.setClientBuffer( b );

            return;
        }

        Casing casing = ((Attachment)e.ipsession().attachment()).casing;
        Pipeline pipeline = ((Attachment)e.ipsession().attachment()).pipeline;

        Long key = new Long(b.getLong());
        Token tok = (Token)pipeline.detach(key);

        if (logger.isDebugEnabled()) {
            logger.debug("RETRIEVED object: " + tok + " with key: " + key + " on pipeline: " + pipeline);
        }

        b.limit(TOKEN_SIZE);

        assert !b.hasRemaining();

        UnparseResult ur;
        try {
            ur = unparseToken(session, casing, tok);
        } catch (Exception exn) { /* not just UnparseException */
            logger.error("internal error, closing connection", exn);
            if (s2c) {
                // XXX We don't have a good handle on this
                session.resetClient();
                session.resetServer();
            } else {
                // XXX We don't have a good handle on this
                session.shutdownServer();
                session.resetClient();
            }
            logger.debug("returning DO_NOT_PASS");

            return;
        }

        if (ur.isStreamer()) {
            TCPStreamer ts = ur.getTcpStreamer();
            if (s2c) {
                session.beginClientStream(ts);
            } else {
                session.beginServerStream(ts);
            }

            return;
        } else {
            if (s2c) {
                logger.debug("unparse result to client");
                ByteBuffer[] r = ur.result();
                if (logger.isDebugEnabled()) {
                    for (int i = 0; null != null && i < r.length; i++) {
                        logger.debug("  to client: " + r[i]);
                    }
                }
                
                session.sendDataToClient( r );
                return;
            } else {
                logger.debug("unparse result to server");
                ByteBuffer[] r = ur.result();
                if (logger.isDebugEnabled()) {
                    for (int i = 0; null != r && i < r.length; i++) {
                        logger.debug("  to server: " + r[i]);
                    }
                }
                session.sendDataToServer( r );
                return;
            }
        }
    }

    private UnparseResult unparseToken(NodeTCPSession session, Casing c, Token token)
        throws UnparseException
    {
        Unparser u = c.unparser();

        if (token instanceof Release) {
            Release release = (Release)token;

            finalize( session );
            session.release();

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

    private void parse(TCPChunkEvent e, boolean s2c, boolean last)
    {
        NodeTCPSession session = e.session();
        Casing casing = ((Attachment)e.ipsession().attachment()).casing;
        Pipeline pipeline = ((Attachment)e.ipsession().attachment()).pipeline;

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
                String sessionEndpoints = "[" +
                    session.getProtocol() + " : " + 
                    session.getClientAddr() + ":" + session.getClientPort() + " -> " +
                    session.getServerAddr() + ":" + session.getServerPort() + "]";

                /**
                 * Some Special handling for semi-common parse exceptions
                 * Otherwise just print the full stack trace
                 *
                 * Parse exception are quite common in the real world as people use non-compliant
                 * or different protocol on standard ports.
                 * As such we don't want to litter the logs too much with these warnings, but we don't want to eliminate
                 * them entirely.
                 *
                 * XXX These are all HTTP based so they should live in the http-casing somewhere
                 */
                String message = exn.getMessage();
                if (message != null && message.contains("no digits found")) {
                    logger.info("Protocol parse exception (no digits found). Releasing session: " + sessionEndpoints);
                } else if (message != null && message.contains("expected")) {
                    logger.info("Protocol parse exception (got != expected). Releasing session: " + sessionEndpoints);
                } else if (message != null && message.contains("data trapped")) {
                    logger.info("Protocol parse exception (data trapped). Releasing session: " + sessionEndpoints, exn);
                    // "data trapped" means that we've already buffered data, and have no discovered its probably not
                    // a protocol we can understand.
                    // Since we've already buffered data we need to reset the bytebuffer to send the data we've already buffered
                    // to do se reset the position to zero, and the the limit to the current position.
                    // Bug #11886 for more details
                    dup.limit(dup.position());
                    dup.position(0);
                } else if (message != null && message.contains("buf limit exceeded")) {
                    logger.info("Protocol parse exception (buf limit exceeded). Releasing session: " + sessionEndpoints);
                } else if (message != null && message.contains("header exceeds")) {
                    logger.info("Protocol parse exception (header exceeds). Releasing session: " + sessionEndpoints);
                } else if (message != null && message.contains("length exceeded")) {
                    logger.info("Protocol parse exception (request length exceeded). Releasing session: " + sessionEndpoints);
                } else if (message != null && message.contains("invalid method")) {
                    logger.info("Protocol parse exception (invalid request method). Releasing session: " + sessionEndpoints);                    
                } else {
                    logger.info("Protocol parse exception. releasing session: " + sessionEndpoints, exn);
                }
                
                finalize( session );
                session.release();

                pr = new ParseResult(new Release(dup));
            } else {
                session.shutdownServer();
                session.shutdownClient();
                return;
            }
        }

        if (pr.isStreamer()) {
            TokenStreamer tokSt = pr.getTokenStreamer();
            TCPStreamer ts = new TokenStreamerAdaptor(pipeline, tokSt);
            if (s2c) {
                session.beginClientStream(ts);
                session.setServerBuffer( pr.getReadBuffer() );
            } else {
                session.beginServerStream(ts);
                session.setClientBuffer( pr.getReadBuffer() );
            }

            return;
        } else {
            List<Token> results = pr.getResults();

            // XXX add magic:
            ByteBuffer bb = ByteBuffer.allocate(TOKEN_SIZE * results.size());

            // XXX add magic:
            for (Token t : results) {
                Long key = pipeline.attach(t);
                if (logger.isDebugEnabled()) {
                    logger.debug("SAVED object: " + t + " with key: " + key + " on pipeline: " + pipeline);
                }
                bb.putLong(key);
            }
            bb.flip();

            ByteBuffer[] r = new ByteBuffer[] { bb };

            if (s2c) {
                if (logger.isDebugEnabled()) {
                    logger.debug("parse result to server, read buffer: " + pr.getReadBuffer() + "  to client: " + r[0]);
                }

                session.sendDataToClient( r );
                session.setServerBuffer( pr.getReadBuffer() );

                return;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("parse result to client, read buffer: " + pr.getReadBuffer() + "  to server: " + r[0]);
                }

                session.sendDataToServer( r );
                session.setClientBuffer( pr.getReadBuffer() );

                return;
            }
        }
    }

    private void finalize( NodeSession sess )
    {
        Casing casing = ((Attachment)sess.attachment()).casing;

        casing.parser().handleFinalized();
        casing.unparser().handleFinalized();
        //removeCasingDesc( sess );
    }

    protected static class Attachment
    {
        final Casing casing;
        final Pipeline pipeline;

        Attachment(Casing casing, Pipeline pipeline)
        {
            this.casing = casing;
            this.pipeline = pipeline;
        }
    }

}
