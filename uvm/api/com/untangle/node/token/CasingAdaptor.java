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
import com.untangle.uvm.vnet.TCPStreamer;

/**
 * Adapts a Token session's underlying byte-stream a <code>Casing</code>.
 */
public class CasingAdaptor extends CasingBase
{
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
    }

    @Override
    public void handleTCPClientChunk( NodeTCPSession session, ByteBuffer data )
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling client chunk, session: " + session.id());
        }

        if (clientSide) {
            parse( session, data, false, false );
            return;
        } else {
            logger.warn("Received data when expect object");
            throw new RuntimeException("Received data when expect object");
        }
    }

    @Override
    public void handleTCPServerChunk( NodeTCPSession session, ByteBuffer data )
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling server chunk, session: " + session.id());
        }

        if (clientSide) {
            logger.warn("Received data when expect object");
            throw new RuntimeException("Received data when expect object");
        } else {
            parse( session, data, true, false );
            return;
        }
    }

    @Override
    public void handleTCPClientObject( NodeTCPSession session, Object obj )
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling client object, session: " + session.id());
        }

        if (clientSide) {
            logger.warn("Received object but expected data.");
            throw new RuntimeException("Received object but expected data.");
        } else {
            unparse( session, obj, false );
            return;
        }
    }
    
    @Override
    public void handleTCPServerObject( NodeTCPSession session, Object obj )
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling server object, session: " + session.id());
        }

        if (clientSide) {
            unparse( session, obj, true );
            return;
        } else {
            logger.warn("Received object but expected data.");
            throw new RuntimeException("Received object but expected data.");
        }
    }
    
    @Override
    public void handleTCPClientDataEnd( NodeTCPSession session, ByteBuffer data )
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling client chunk, session: " + session.id());
        }

        if (clientSide) {
            parse( session, data, false, true);
            return;
        } else {
            if ( data.hasRemaining() ) {
                logger.warn("Received data when expect object");
                throw new RuntimeException("Received data when expect object");
            }
            return;
        }
    }

    @Override
    public void handleTCPServerDataEnd( NodeTCPSession session, ByteBuffer data )
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling server chunk, session: " + session.id());
        }

        if (clientSide) {
            if ( data.hasRemaining() ) {
                logger.warn("Received data when expect object");
                throw new RuntimeException("Received data when expect object");
            }
        } else {
            parse( session, data, true, true );
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

    private void unparse( NodeTCPSession session, Object obj, boolean s2c )
    {
        Casing casing = (Casing)session.attachment();

        Token tok = (Token) obj;
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

                if (result.length > 0) {
                    session.sendDataToClient( result );
                }
                return;
            } else {
                logger.debug("unparse result to server");
                ByteBuffer[] result = ur.result();
                if (logger.isDebugEnabled()) {
                    for (int i = 0; result != null && i < result.length; i++) {
                        logger.debug("  to server: " + result[i]);
                    }
                }
                if (result.length > 0) {
                    session.sendDataToServer( result );
                }
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
                TCPStreamer ts = new ReleaseTcpStreamer(ur.getTcpStreamer(), release);
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

    private void parse( NodeTCPSession session, ByteBuffer data, boolean s2c, boolean last )
    {
        Casing casing = (Casing)session.attachment();

        ParseResult pr;
        ByteBuffer buf = data;
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

            if (s2c) {
                if (logger.isDebugEnabled()) {
                    logger.debug("parse result to server, read buffer: " + pr.getReadBuffer() + "  to client: " + results);
                }

                if ( results.size() > 0 ) {
                    Token[] arr = results.toArray( new Token[results.size()] );
                    session.sendObjectsToClient( arr );
                }
                session.setServerBuffer( pr.getReadBuffer() );
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("parse result to client, read buffer: " + pr.getReadBuffer() + "  to server: " + results);
                }

                if ( results.size() > 0 ) {
                    Token[] arr = results.toArray( new Token[results.size()] );
                    session.sendObjectsToServer( arr );
                }
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
