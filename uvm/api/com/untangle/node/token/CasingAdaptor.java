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
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.event.TCPChunkEvent;
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
    public void handleTCPNewSession( NodeTCPSession session )
    {
        Casing casing = casingFactory.casing( session, clientSide );

        session.attach( casing );
        
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
    public void handleTCPClientFIN( NodeTCPSession session )
    {
        TCPStreamer tcpStream = null;

        Casing casing = (Casing)session.attachment();

        if (clientSide) {
            TokenStreamer tokSt = casing.parser().endSession();
            if (null != tokSt) {
                tcpStream = new TokenStreamerAdaptor( tokSt, session );
            }
        } else {
            tcpStream = casing.unparser().endSession();
        }

        if (null != tcpStream) {
            session.beginServerStream(tcpStream);
        } else {
            session.shutdownServer();
        }
    }

    @Override
    public void handleTCPServerFIN( NodeTCPSession session )
    {
        TCPStreamer ts = null;

        Casing casing = (Casing)session.attachment();

        if (clientSide) {
            ts = casing.unparser().endSession();
        } else {
            TokenStreamer tokSt = casing.parser().endSession();
            if (null != tokSt) {
                ts = new TokenStreamerAdaptor( tokSt, session );
            }
        }

        if (null != ts) {
            session.beginClientStream(ts);
        } else {
            session.shutdownClient();
        }
    }

    @Override
    public void handleTCPFinalized( NodeTCPSession session ) 
    {
        if (logger.isDebugEnabled()) {
            logger.debug("finalizing " + session.id());
        }
        finalize( session );
    }

    @Override
    public void handleTimer( NodeSession sess )
    {
        Casing casing = (Casing) sess.attachment();

        Parser p = casing.parser();
        p.handleTimer();
        // XXX unparser doesnt get one, does it need it?
    }

    // private methods --------------------------------------------------------

    private void unparse(TCPChunkEvent e, boolean s2c)
    {
        ByteBuffer chunk = e.chunk();
        NodeTCPSession session = e.session();

        if (chunk.remaining() < TOKEN_SIZE) {
            // read limit 2
            chunk.compact();
            chunk.limit(TOKEN_SIZE);
            if (logger.isDebugEnabled()) {
                logger.debug("unparse returning buffer, for more: " + chunk);
            }
            if ( s2c )
                session.setServerBuffer( chunk );
            else
                session.setClientBuffer( chunk );
            return;
        }

        Casing casing = (Casing)e.session().attachment();

        Long key = new Long(chunk.getLong());
        Token tok = (Token) session.globalAttachment( key );
        session.globalAttach( key, null ); // remove key

        if (logger.isDebugEnabled()) {
            logger.debug("RETRIEVED object: " + tok + " with key: " + key );
        }

        chunk.limit(TOKEN_SIZE);

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
                ByteBuffer[] result = ur.result();
                if (logger.isDebugEnabled()) {
                    for (int i = 0; result != null && i < result.length; i++) {
                        logger.debug("  to client: " + result[i]);
                    }
                }

                if (result.length > 0)
                    session.sendDataToClient( result );
                return;
            } else {
                logger.debug("unparse result to server");
                ByteBuffer[] result = ur.result();
                if (logger.isDebugEnabled()) {
                    for (int i = 0; result != null && i < result.length; i++) {
                        logger.debug("  to server: " + result[i]);
                    }
                }
                if (result.length > 0)
                    session.sendDataToServer( result );
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
        Casing casing = (Casing)e.session().attachment();

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
            TCPStreamer ts = new TokenStreamerAdaptor(tokSt, session);
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

            ByteBuffer bb = ByteBuffer.allocate(TOKEN_SIZE * results.size());

            for (Token t : results) {
                Long key = session.getUniqueGlobalAttachmentKey();
                session.globalAttach(key, t);
                if (logger.isDebugEnabled()) {
                    logger.debug("SAVED object: " + t + " with key: " + key );
                }
                bb.putLong(key);
            }
            bb.flip();

            if (s2c) {
                if (logger.isDebugEnabled()) {
                    logger.debug("parse result to server, read buffer: " + pr.getReadBuffer() + "  to client: " + bb);
                }

                if ( results.size() > 0 )
                    session.sendDataToClient( bb );
                session.setServerBuffer( pr.getReadBuffer() );
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("parse result to client, read buffer: " + pr.getReadBuffer() + "  to server: " + bb);
                }

                if ( results.size() > 0 )
                    session.sendDataToServer( bb );
                session.setClientBuffer( pr.getReadBuffer() );
            }
            return;
        }
    }

    private void finalize( NodeSession sess )
    {
        Casing casing = (Casing)sess.attachment();;

        casing.parser().handleFinalized();
        casing.unparser().handleFinalized();
    }
}
