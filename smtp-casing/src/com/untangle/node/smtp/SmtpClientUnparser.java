/**
 * $Id$
 */
package com.untangle.node.smtp;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.Response;
import com.untangle.node.smtp.SASLExchangeToken;
import com.untangle.node.token.MetadataToken;
import com.untangle.node.token.Token;
import com.untangle.node.token.UnparseResult;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * ...name says it all...
 */
class SmtpClientUnparser extends SmtpUnparser
{

    private final Logger m_logger = Logger.getLogger(SmtpClientUnparser.class);

    SmtpClientUnparser(NodeTCPSession session, SmtpCasing parent, CasingSessionTracker tracker) {
        super(session, true, parent, tracker);
        m_logger.debug("Created");
    }

    @Override
    protected UnparseResult doUnparse(Token token)
    {

        // -----------------------------------------------------------
        if (token instanceof SASLExchangeToken) {
            m_logger.debug("Received SASLExchangeToken token");

            ByteBuffer buf = token.getBytes();

            if (!getCasing().isInSASLLogin()) {
                m_logger.error("Received SASLExchangeToken without an open exchange");
            } else {
                switch (getCasing().getSASLObserver().serverData(buf.duplicate())) {
                    case EXCHANGE_COMPLETE:
                        m_logger.debug("SASL Exchange complete");
                        getCasing().closeSASLExchange();
                        break;
                    case IN_PROGRESS:
                        // Nothing to do
                        break;
                    case RECOMMEND_PASSTHRU:
                        m_logger.debug("Entering passthru on advice of SASLObserver");
                        declarePassthru();
                }
            }
            return new UnparseResult(buf);
        }

        // -----------------------------------------------------------
        if (token instanceof MetadataToken) {
            // Don't pass along metadata tokens
            m_logger.debug("Pass along Metadata token as nothing");
            return UnparseResult.NONE;
        }

        // -----------------------------------------------------------
        if (token instanceof Response) {
            Response resp = (Response) token;
            getSessionTracker().responseReceived(resp);

            m_logger.debug("Passing response to client: " + resp.toDebugString());
        } else {
            m_logger.debug("Unparse token of type " + (token == null ? "null" : token.getClass().getName()));
        }

        return new UnparseResult(token.getBytes());
    }

}
