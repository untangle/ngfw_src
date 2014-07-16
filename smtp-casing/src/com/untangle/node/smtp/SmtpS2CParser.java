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
import com.untangle.node.token.Chunk;
import com.untangle.node.token.ParseResult;
import com.untangle.node.token.PassThruToken;
import com.untangle.node.token.Token;
import com.untangle.uvm.vnet.NodeTCPSession;

class SmtpS2CParser extends SmtpParser
{
    private final Logger logger = Logger.getLogger(SmtpS2CParser.class);

    public SmtpS2CParser( )
    {
        super( false );
    }

    public void handleNewSession( NodeTCPSession session )
    {
        lineBuffering( session, false );
    }
    
    @Override
    @SuppressWarnings("fallthrough")
    protected ParseResult doParse( NodeTCPSession session, ByteBuffer buf )
    {
        List<Token> toks = new ArrayList<Token>();
        boolean done = false;
        SmtpSharedState sharedState = (SmtpSharedState) session.globalAttachment( SHARED_STATE_KEY );

        while (buf.hasRemaining() && !done) {

            if ( isPassthru( session ) ) {
                logger.debug("Passthru buffer (" + buf.remaining() + " bytes )");
                toks.add(new Chunk(buf));
                return new ParseResult(toks);
            }

            if ( sharedState.isInSASLLogin() ) {
                logger.debug("In SASL Exchange");
                ByteBuffer dup = buf.duplicate();
                switch ( sharedState.getSASLObserver().serverData( buf ) ) {
                    case EXCHANGE_COMPLETE:
                        logger.debug("SASL Exchange complete");
                        sharedState.closeSASLExchange();
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
                        toks.add(new Chunk(dup.slice()));
                        buf.position(buf.limit());
                        return new ParseResult(toks);
                }
                continue;
            }

            try {
                ByteBuffer dup = buf.duplicate();
                Response resp = new ResponseParser().parse(dup);
                if (resp != null) {
                    buf.position(dup.position());
                    sharedState.responseReceived(resp);
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
                toks.add( new Chunk(buf) );
                return new ParseResult(toks);
            }
        }

        // Compact the buffer
        buf = compactIfNotEmpty(buf, (1024 * 2));

        if (buf != null) {
            logger.debug("returning ParseResult with " + toks.size() + " tokens and a buffer with " + buf.remaining()
                    + " remaining");
        }
        return new ParseResult(toks, buf);
    }

}
