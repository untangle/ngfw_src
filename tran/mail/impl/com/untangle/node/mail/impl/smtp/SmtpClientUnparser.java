/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.mail.impl.smtp;

import java.nio.ByteBuffer;

import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.mail.papi.smtp.Response;
import com.untangle.tran.mail.papi.smtp.SASLExchangeToken;
import com.untangle.tran.token.MetadataToken;
import com.untangle.tran.token.Token;
import com.untangle.tran.token.UnparseResult;
import org.apache.log4j.Logger;

/**
 * ...name says it all...
 */
class SmtpClientUnparser
    extends SmtpUnparser {

    private final Logger m_logger =
        Logger.getLogger(SmtpClientUnparser.class);

    SmtpClientUnparser(TCPSession session,
                       SmtpCasing parent,
                       CasingSessionTracker tracker) {
        super(session, parent, tracker, true);
        m_logger.debug("Created");
    }


    @Override
    protected UnparseResult doUnparse(Token token) {

        //-----------------------------------------------------------
        if(token instanceof SASLExchangeToken) {
            m_logger.debug("Received SASLExchangeToken token");

            ByteBuffer buf = token.getBytes();

            if(!getSmtpCasing().isInSASLLogin()) {
                m_logger.error("Received SASLExchangeToken without an open exchange");
            }
            else {
                switch(getSmtpCasing().getSASLObserver().serverData(buf.duplicate())) {
                case EXCHANGE_COMPLETE:
                    m_logger.debug("SASL Exchange complete");
                    getSmtpCasing().closeSASLExchange();
                case IN_PROGRESS:
                    //Nothing to do
                    break;
                case RECOMMEND_PASSTHRU:
                    m_logger.debug("Entering passthru on advice of SASLObserver");
                    declarePassthru();
                }
            }
            return new UnparseResult(buf);
        }

        //-----------------------------------------------------------
        if(token instanceof MetadataToken) {
            //Don't pass along metadata tokens
            m_logger.debug("Pass along Metadata token as nothing");
            return UnparseResult.NONE;
        }

        //-----------------------------------------------------------
        if(token instanceof Response) {
            Response resp = (Response) token;
            getSessionTracker().responseReceived(resp);

            m_logger.debug("Passing response to client: " +
                           resp.toDebugString());
        }
        else {
            m_logger.debug("Unparse token of type " + (token==null?"null":token.getClass().getName()));
        }

        return new UnparseResult(token.getBytes());
    }


}
