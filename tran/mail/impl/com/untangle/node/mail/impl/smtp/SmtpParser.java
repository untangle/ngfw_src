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
import com.untangle.tran.mail.impl.AbstractMailParser;
import org.apache.log4j.Logger;


/**
 * Base class for the SmtpClient/ServerParser
 */
abstract class SmtpParser
    extends AbstractMailParser {

    private final Logger m_logger =
        Logger.getLogger(SmtpParser.class);
    private boolean m_passthru = false;
    private CasingSessionTracker m_tracker;

    protected  SmtpParser(TCPSession session,
                          SmtpCasing parent,
                          CasingSessionTracker tracker,
                          boolean clientSide) {

        super(session, parent, clientSide, "smtp");
        m_tracker = tracker;
    }

    SmtpCasing getSmtpCasing() {
        return (SmtpCasing) getParentCasing();
    }
    CasingSessionTracker getSessionTracker() {
        return m_tracker;
    }

    /**
     * Helper which compacts (and possibly expands)
     * the buffer if anything remains.  Otherwise,
     * just returns null.
     */
    protected static ByteBuffer compactIfNotEmpty(ByteBuffer buf,
                                                  int maxSz) {
        if(buf.hasRemaining()) {
            buf.compact();
            if(buf.limit() < maxSz) {
                ByteBuffer b = ByteBuffer.allocate(maxSz);
                buf.flip();
                b.put(buf);
                return b;
            }
            return buf;
        }
        else {
            return null;
        }
    }

}
