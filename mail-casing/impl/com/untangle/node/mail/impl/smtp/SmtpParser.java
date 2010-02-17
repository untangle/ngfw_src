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

import com.untangle.node.mail.impl.AbstractMailParser;
import com.untangle.uvm.vnet.TCPSession;


/**
 * Base class for the SmtpClient/ServerParser
 */
abstract class SmtpParser
    extends AbstractMailParser {

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
