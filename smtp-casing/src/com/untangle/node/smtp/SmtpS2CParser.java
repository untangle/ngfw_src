/**
 * $Id$
 */
package com.untangle.node.smtp;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.Response;
import com.untangle.node.smtp.ResponseParser;
import com.untangle.node.smtp.SASLExchangeToken;
import com.untangle.node.token.ChunkToken;
import com.untangle.node.token.PassThruToken;
import com.untangle.node.token.Token;
import com.untangle.uvm.vnet.NodeTCPSession;

class SmtpS2CParser extends SmtpParser
{
    private final Logger logger = Logger.getLogger(SmtpS2CParser.class);

    public SmtpS2CParser()
    {
        super( false );
    }

    public void handleNewSession( NodeTCPSession session )
    {
        SmtpSharedState serverSideSharedState = new SmtpSharedState();
        session.attach( SHARED_STATE_KEY, serverSideSharedState );

        lineBuffering( session, false );
    }
    
    @Override
    @SuppressWarnings("fallthrough")
    protected void doParse( NodeTCPSession session, ByteBuffer buf )
    {
        List<Token> toks = new ArrayList<Token>();
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

}
