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

package com.untangle.tran.mail.papi.imap;

import com.untangle.mvvm.tapi.event.TCPStreamer;
import com.untangle.tran.mail.papi.BeginMIMEToken;
import com.untangle.tran.mail.papi.MIMEAccumulator;
import com.untangle.tran.mail.papi.MessageInfo;


/**
 * Token reprsenting the Begining
 * of a MIME message in Imap.
 * Adds the {@link #getMessageLength Message Length}
 * attribute to the base BeginMIMEToken and adds
 * a new method for {@link #toImapTCPStreamer streaming in the unparser}.
 *
 */
public class BeginImapMIMEToken
    extends BeginMIMEToken {

    private final int m_msgLen;

    public BeginImapMIMEToken(MIMEAccumulator accumulator,
                              MessageInfo messageInfo,
                              int msgLen) {
        super(accumulator, messageInfo);
        m_msgLen = msgLen;
    }

    /**
     * Gets the Message length (from the LITERAL declaration
     * of octets in IMAP).
     *
     * @return the message length
     */
    public int getMessageLength() {
        return m_msgLen;
    }

    /**
     * Get a TCPStreamer for the initial contents
     * of this message, including the opening literal declaration.
     *
     * @return the TCPStreamer
     */
    public TCPStreamer toImapTCPStreamer(boolean disposeAccumulatorWhenDone) {
        return new CLLTCPStreamer(
                                  super.toUnstuffedTCPStreamer(),
                                  getMessageLength(),
                                  disposeAccumulatorWhenDone);
    }

    /**
     * Little class which closes the MIMEAccumulator
     * when completed.
     */
    class CLLTCPStreamer
        extends LiteralLeadingTCPStreamer {

        private final boolean m_disposeAccumulatorWhenDone;

        CLLTCPStreamer(TCPStreamer wrapped,
                       int literalLength,
                       boolean disposeAccumulatorWhenDone) {
            super(wrapped, literalLength);
            m_disposeAccumulatorWhenDone = disposeAccumulatorWhenDone;
        }

        @Override
        protected void doneStreaming() {
            if(m_disposeAccumulatorWhenDone && getMIMEAccumulator() != null) {
                getMIMEAccumulator().dispose();
            }
        }
    }
}
