 /**
 * $Id$
 */
package com.untangle.app.smtp;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.app.smtp.Response;
import com.untangle.app.smtp.ResponseParser;
import com.untangle.app.smtp.SASLExchangeToken;
import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.ReleaseToken;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AbstractEventHandler;

/**
 * SMTP server parserer for events.
 */
public class SmtpServerParserEventHandler extends AbstractEventHandler
{
    protected static final String SHARED_STATE_KEY = "SMTP-shared-state";

    private final Logger logger = Logger.getLogger(SmtpServerParserEventHandler.class);
    
    /**
     * Initialize SmtpServerParserEventHandler.
     * @return SmtpServerParserEventHandler instance.
     */
    public SmtpServerParserEventHandler()
    {
        super();
    }

    /**
     * Process new session.
     * @param session AppTCPSession to process.
     */
    @Override
    public void handleTCPNewSession( AppTCPSession session )
    {
        SmtpSharedState serverSideSharedState = new SmtpSharedState();
        session.attach( SHARED_STATE_KEY, serverSideSharedState );
    }

    /**
     * Process session chunk from client.
     * @param session AppTCPSession to process.
     * @param data ByteBuffer to process.
     */
    @Override
    public void handleTCPClientChunk( AppTCPSession session, ByteBuffer data )
    {
        // grab the SSL Inspector status attachment and release if set to false
        Boolean sslInspectorStatus = (Boolean)session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SESSION_INSPECT);

        if ((sslInspectorStatus != null) && (sslInspectorStatus.booleanValue() == false)) {
            session.sendDataToServer(data);
            session.release();
            return;
        }

        logger.warn("Received data when expect object");
        throw new RuntimeException("Received data when expect object");
    }

    /**
     * Process session chunk from server.
     * @param session AppTCPSession to process.
     * @param data ByteBuffer to process.
     */
    @Override
    public void handleTCPServerChunk( AppTCPSession session, ByteBuffer data )
    {
        // grab the SSL Inspector status attachment and release if set to false
        Boolean sslInspectorStatus = (Boolean)session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SESSION_INSPECT);

        if ((sslInspectorStatus != null) && (sslInspectorStatus.booleanValue() == false)) {
            session.sendDataToClient(data);
            session.release();
            return;
        }

        parse( session, data, true, false );
    }

    /**
     * Process session object from client.
     * @param session AppTCPSession to process.
     * @param obj Object to process.
     */
    @Override
    public void handleTCPClientObject( AppTCPSession session, Object obj )
    {
        // grab the SSL Inspector status attachment and release if set to false
        Boolean sslInspectorStatus = (Boolean)session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SESSION_INSPECT);

        if ((sslInspectorStatus != null) && (sslInspectorStatus.booleanValue() == false)) {
            session.release();
            return;
        }

        logger.warn("Received object but expected data.");
        throw new RuntimeException("Received object but expected data.");
    }
    
    /**
     * Process session object from server.
     * @param session AppTCPSession to process.
     * @param obj Object to process.
     */
    @Override
    public void handleTCPServerObject( AppTCPSession session, Object obj )
    {
        // grab the SSL Inspector status attachment and release if set to false
        Boolean sslInspectorStatus = (Boolean)session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SESSION_INSPECT);

        if ((sslInspectorStatus != null) && (sslInspectorStatus.booleanValue() == false)) {
            session.release();
            return;
        }

        logger.warn("Received object but expected data.");
        throw new RuntimeException("Received object but expected data.");
    }
    
    /**
     * Process end of session from client.
     * @param session AppTCPSession to process.
     * @param data ByteBuffer to process.
     */
    @Override
    public void handleTCPClientDataEnd( AppTCPSession session, ByteBuffer data )
    {
        if ( data.hasRemaining() ) {
            logger.warn("Received data when expect object");
            throw new RuntimeException("Received data when expect object");
        }
    }

    /**
     * Process end of session from server.
     * @param session AppTCPSession to process.
     * @param data ByteBuffer to process.
     */
    @Override
    public void handleTCPServerDataEnd( AppTCPSession session, ByteBuffer data )
    {
        parse( session, data, true, true );
    }

    /**
     * Process session FIN from client.
     * @param session AppTCPSession to process.
     */
    @Override
    public void handleTCPClientFIN( AppTCPSession session )
    {
        logger.warn("Received unexpected event.");
        throw new RuntimeException("Received unexpected event.");
    }

    /**
     * Process session FIN from server.
     * @param session AppTCPSession to process.
     */
    @Override
    public void handleTCPServerFIN( AppTCPSession session )
    {
        session.shutdownClient();
    }
    
    /**
     * Parse SMTP server events.
     * @param session AppTCPSesson to handle.
     * @param data    ByteBuffer  to parse.
     * @param s2c     If true, this is server to client, client to server othwerwise.
     * @param last    If true this is the last item to parse.
     */
    private void parse( AppTCPSession session, ByteBuffer data, boolean s2c, boolean last )
    {
        ByteBuffer buf = data;
        ByteBuffer dup = buf.duplicate();
        try {
            if (last) {
                parseEnd( session, buf );
            } else {
                parse( session, buf );
            }
        } catch (Throwable exn) {
            String sessionEndpoints = "[" +
                session.getProtocol() + " : " + 
                session.getClientAddr() + ":" + session.getClientPort() + " -> " +
                session.getServerAddr() + ":" + session.getServerPort() + "]";
                
            session.release();

            if ( s2c ) {
                session.sendObjectToClient( new ReleaseToken() );
                session.sendDataToClient( dup );
            } else {
                session.sendObjectToServer( new ReleaseToken() );
                session.sendDataToServer( dup );
            }
            return;
        }
    }
    
    /**
     * Parse SMTP server events.
     * @param session AppTCPSesson to handle.
     * @param buf     ByteBuffer  to parse.
     */
    public void parse( AppTCPSession session, ByteBuffer buf )
    {
        try {
            if ( isPassthru( session ) ) {
                session.sendObjectToClient( new ChunkToken(buf) );
                return;
            } else {
                doParse( session, buf );
                return;
            }
        } catch ( Exception exn ) {
            session.shutdownClient();
            session.shutdownServer();
            return;
        }
    }

    /**
     * Stop parsing session.
     * @param session AppTCPSession to end parsing.
     * @param buf     ByteBuffer to parse.
     */
    public final void parseEnd( AppTCPSession session, ByteBuffer buf )
    {
        if ( buf.hasRemaining() ) {
            session.sendObjectToClient( new ChunkToken(buf) );
            return;
        }
        return;
    }
    
    /**
     * Perform parsing on SMTP server session.
     * @param session AppTCPSession to parse.
     * @param buf     ByteBuffer to parse.
     */
    @SuppressWarnings("fallthrough")
    protected void doParse( AppTCPSession session, ByteBuffer buf )
    {
        List<Token> toks = new ArrayList<>();
        boolean done = false;
        SmtpSharedState serverSideSharedState = (SmtpSharedState) session.attachment( SHARED_STATE_KEY );

        while (buf.hasRemaining() && !done) {

            if ( isPassthru( session ) ) {
                logger.debug("Passthru buffer (" + buf.remaining() + " bytes )");
                toks.add(new ChunkToken(buf));
                for ( Token tok : toks )
                    session.sendObjectToClient( tok );
                return;
            }

            if ( serverSideSharedState.isInSASLLogin() ) {
                logger.debug("In SASL Exchange");
                ByteBuffer dup = buf.duplicate();
                switch ( serverSideSharedState.getSASLObserver().serverData( buf ) ) {
                    case EXCHANGE_COMPLETE:
                        logger.debug("SASL Exchange complete");
                        serverSideSharedState.closeSASLExchange();
                        // fallthrough ?? XXX
                    case IN_PROGRESS:
                        // There should not be any extra bytes
                        // left with "in progress", but what the hell
                        dup.limit(buf.position());
                        toks.add(new SASLExchangeToken(dup));
                        break;
                    case RECOMMEND_PASSTHRU:
                        logger.debug("Entering passthru on advice of SASLObserver");
                        declarePassthru( session );
                        toks.add(PassThruToken.PASSTHRU);
                        toks.add(new ChunkToken(dup.slice()));
                        buf.position(buf.limit());
                        for ( Token tok : toks )
                            session.sendObjectToClient( tok );
                        return;
                }
                continue;
            }

            try {
                ByteBuffer dup = buf.duplicate();
                Response resp = new ResponseParser().parse(dup);
                if (resp != null) {
                    buf.position(dup.position());
                    serverSideSharedState.responseReceived(resp);
                    logger.debug("Received response: " + resp.toDebugString());
                    toks.add(resp);
                } else {
                    done = true;
                    logger.debug("Need more bytes for response");
                }
            } catch (Exception ex) {
                logger.warn("Exception parsing server response", ex);
                declarePassthru( session );
                toks.add( PassThruToken.PASSTHRU );
                toks.add( new ChunkToken(buf) );
                for ( Token tok : toks )
                    session.sendObjectToClient( tok );
                return;
            }
        }

        // Compact the buffer
        buf = compactIfNotEmpty(buf, (1024 * 2));

        if (buf != null) {
            logger.debug("sending " + toks.size() + " tokens and setting a buffer with " + buf.remaining() + " remaining");
        }
        for ( Token tok : toks )
            session.sendObjectToClient( tok );
        session.setServerBuffer( buf );
        return;
    }

    /**
     * Helper which compacts (and possibly expands) the buffer if anything remains. Otherwise, just returns null.
     * @param buf   ByteBuffer to compact.
     * @param maxSz Maximum size to create.
     * @return Compacted ByteBuffer.
     */
    protected static ByteBuffer compactIfNotEmpty(ByteBuffer buf, int maxSz)
    {
        if (buf.hasRemaining()) {
            buf.compact();
            if (buf.limit() < maxSz) {
                ByteBuffer b = ByteBuffer.allocate(maxSz);
                buf.flip();
                b.put(buf);
                return b;
            }
            return buf;
        } else {
            return null;
        }
    }
    
    /**
     * Is the casing currently in passthru mode
     * @param session AppTCPSession to check.
     * @return If true, is passthu, otherwise not passthru.
     */
    protected boolean isPassthru( AppTCPSession session )
    {
        SmtpSharedState sharedState = (SmtpSharedState) session.attachment( SHARED_STATE_KEY );
        return sharedState.passthru;
    }

    /**
     * Called by the unparser to declare that we are now in passthru mode. This is called either because of a parsing
     * error by the caller, or the reciept of a passthru token.
     * @param session AppTCPSession to set passthru on.
     */
    protected void declarePassthru( AppTCPSession session)
    {
        SmtpSharedState sharedState = (SmtpSharedState) session.attachment( SHARED_STATE_KEY );
        sharedState.passthru = true;
    }
    
}
