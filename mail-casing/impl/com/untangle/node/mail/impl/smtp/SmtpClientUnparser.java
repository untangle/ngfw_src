/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.mail.impl.smtp;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.smtp.Response;
import com.untangle.node.mail.papi.smtp.SASLExchangeToken;
import com.untangle.node.token.MetadataToken;
import com.untangle.node.token.Token;
import com.untangle.node.token.UnparseResult;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * ...name says it all...
 */
class SmtpClientUnparser
    extends SmtpUnparser {

    private final Logger m_logger =
        Logger.getLogger(SmtpClientUnparser.class);

    SmtpClientUnparser(NodeTCPSession session,
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
                    break;
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
