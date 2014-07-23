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
public class CasingAdaptor extends AbstractEventHandler
{
    protected final Logger logger = Logger.getLogger(CasingAdaptor.class);

    protected final Parser parser;
    protected final Unparser unparser;
    protected final boolean clientSide;
    protected volatile boolean releaseParseExceptions;
    
    public CasingAdaptor( Node node, Parser parser, Unparser unparser, boolean clientSide, boolean releaseParseExceptions )
    {
        super(node);
        this.parser = parser;
        this.unparser = unparser;
        this.clientSide = clientSide;
        this.releaseParseExceptions = releaseParseExceptions;
    }

    // SessionEventHandler methods -------------------------------------------

    @Override
    public void handleTCPNewSession( NodeTCPSession session )
    {
        this.parser.handleNewSession( session );
        this.unparser.handleNewSession( session );
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
        if (clientSide) {
            this.parser.endSession( session );
        } else {
            this.unparser.endSession( session );
        }

        return;
    }

    @Override
    public void handleTCPServerFIN( NodeTCPSession session )
    {
        if (clientSide) {
            this.unparser.endSession( session );
        } else {
            this.parser.endSession( session );
        }

        return;
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
    public void handleTimer( NodeSession session )
    {
        // Casing casing = (Casing) sess.attachment();

        // Parser p = casing.parser();
        this.parser.handleTimer( session );
        // XXX unparser doesnt get one, does it need it?
    }

    // private methods --------------------------------------------------------

    private void unparse( NodeTCPSession session, Object obj, boolean s2c )
    {
        // Casing casing = (Casing)session.attachment();

        Token tok = (Token) obj;

        try {
            unparseToken(session, this.unparser, tok);
        } catch (Exception exn) {
            logger.error("internal error, closing connection", exn);

            session.resetClient();
            session.resetServer();

            return;
        }
    }

    private void unparseToken( NodeTCPSession session, Unparser unparser, Token token ) throws Exception
    {
        if (token instanceof ReleaseToken) {
            ReleaseToken release = (ReleaseToken)token;

            finalize( session );
            session.release();

            unparser.releaseFlush( session );
            return;
        } else {
            unparser.unparse( session, token );
            return;
        }
    }

    private void parse( NodeTCPSession session, ByteBuffer data, boolean s2c, boolean last )
    {
        ByteBuffer buf = data;
        ByteBuffer dup = buf.duplicate();
        Parser p = this.parser;
        try {
            if (last) {
                p.parseEnd( session, buf );
            } else {
                p.parse( session, buf );
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

                if ( s2c ) {
                    session.sendObjectToClient( new ReleaseToken( dup ) );
                } else {
                    session.sendObjectToServer( new ReleaseToken( dup ) );
                }
                return;
            } else {
                session.shutdownServer();
                session.shutdownClient();
                return;
            }
        }


        return;
    }

    private void finalize( NodeTCPSession session )
    {
        this.parser.handleFinalized( session );
        this.unparser.handleFinalized( session );
    }
}
