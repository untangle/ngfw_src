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

package com.untangle.node.mail.impl.imap;

import java.nio.ByteBuffer;

import com.untangle.node.mail.impl.AbstractMailParser;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Base class for the ImapClient/ServerParser
 */
abstract class ImapParser
    extends AbstractMailParser {

    ImapParser(NodeTCPSession session,
               ImapCasing parent,
               boolean clientSide) {

        super(session, parent, clientSide, "imap");
    }

    /**
     * Accessor for the parent casing
     */
    protected ImapCasing getImapCasing() {
        return (ImapCasing) getParentCasing();
    }

    /**
     * Helper which compacts (and possibly expands)
     * the buffer if anything remains.  Otherwise,
     * just returns null.
     */
    protected static ByteBuffer compactIfNotEmpty(ByteBuffer buf,
                                                  int maxTokenSz) {
        if(buf.hasRemaining()) {
            //Note - do not compact, copy instead.  There was an issue
            //w/ the original buffer being passed as tokens (and we were modifying
            //the head).
            ByteBuffer ret = ByteBuffer.allocate(maxTokenSz+1024);
            ret.put(buf);
            return ret;

        }
        else {
            return null;
        }
    }

}
