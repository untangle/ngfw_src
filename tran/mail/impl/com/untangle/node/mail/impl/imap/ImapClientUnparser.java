/**
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.node.mail.impl.imap;

import java.nio.ByteBuffer;

import com.untangle.uvm.tapi.TCPSession;
import com.untangle.node.mail.papi.ContinuedMIMEToken;
import com.untangle.node.mail.papi.imap.BeginImapMIMEToken;
import com.untangle.node.mail.papi.imap.CompleteImapMIMEToken;
import com.untangle.node.mail.papi.imap.ImapChunk;
import com.untangle.node.mail.papi.imap.UnparsableMIMEChunk;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.Token;
import com.untangle.node.token.UnparseResult;
import org.apache.log4j.Logger;

/**
 * ...name says it all...
 */
class ImapClientUnparser
    extends ImapUnparser {

    private final Logger m_logger =
        Logger.getLogger(ImapClientUnparser.class);

    ImapClientUnparser(TCPSession session,
                       ImapCasing parent) {
        super(session, parent, true);
        m_logger.debug("Created");
    }


    @Override
    protected UnparseResult doUnparse(Token token) {

        if(token instanceof UnparsableMIMEChunk) {
            m_logger.debug("Unparsing UnparsableMIMEChunk");
            ByteBuffer buf = ((UnparsableMIMEChunk)token).getBytes();
            //Do not bother giving these to the session monitor
            return new UnparseResult(buf);
        }
        if(token instanceof ImapChunk) {
            m_logger.debug("Unparsing ImapChunk");
            ByteBuffer buf = ((ImapChunk)token).getBytes();
            if(getImapCasing().getSessionMonitor().bytesFromServer(buf.duplicate())) {
                if(!isPassthru()) {
                    m_logger.warn("Declaring passthru on advice of SessionMonitor, yet " +
                                  "should have already been declared by other half of casing");
                    declarePassthru();
                }
            }
            return new UnparseResult(buf);
        }
        if(token instanceof Chunk) {
            //This should only follow declaration of "PASSTHRU"
            if(!isPassthru()) {
                m_logger.warn("Unparsing Chunk (unexpected)");
            }
            else {
                m_logger.debug("Unparsing Chunk");
            }
            ByteBuffer buf = ((Chunk)token).getBytes();
            return new UnparseResult(buf);
        }
        if(token instanceof BeginImapMIMEToken) {
            m_logger.debug("Unparsing BeginImapMIMEToken");
            return new UnparseResult(
                                     ((BeginImapMIMEToken) token).toImapTCPStreamer(true));
        }
        if(token instanceof ContinuedMIMEToken) {
            ContinuedMIMEToken cmt = (ContinuedMIMEToken) token;
            if(cmt.shouldUnparse()) {
                m_logger.debug("Unparsing ContinuedMIMEToken (" +
                               (cmt.isLast()?"last)":"not last)"));
                ByteBuffer buf = cmt.getBytes();
                return new UnparseResult(buf);
            }
            else {
                m_logger.debug("Unparsing ContinuedMIMEToken (" +
                               (cmt.isLast()?"last":"not last") + ") with nothing to unparse");
                return UnparseResult.NONE;
            }
        }
        if(token instanceof CompleteImapMIMEToken) {
            m_logger.debug("Unparsing CompleteImapMIMEToken");
            return new UnparseResult(
                                     ((CompleteImapMIMEToken) token).toImapTCPStreamer(getPipeline(),
                                                                                       true)
                                     );
        }
        m_logger.warn("Unknown token type: " + token.getClass().getName());
        return new UnparseResult(token.getBytes());

    }

}
