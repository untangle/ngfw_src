/**
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

import org.apache.log4j.Logger;

import com.untangle.node.token.Chunk;
import com.untangle.node.token.Token;
import com.untangle.node.token.UnparseResult;
import com.untangle.uvm.vnet.TCPSession;

/**
 * ...name says it all...
 */
class ImapServerUnparser
    extends ImapUnparser {

    private final Logger m_logger =
        Logger.getLogger(ImapServerUnparser.class);

    ImapServerUnparser(TCPSession session,
                       ImapCasing parent) {
        super(session, parent, false);
        m_logger.debug("Created");
    }


    @Override
    protected UnparseResult doUnparse(Token token) {

        ByteBuffer buf = ((Chunk)token).getBytes();

        if(!isPassthru()) {
            if(getImapCasing().getSessionMonitor().bytesFromClient(buf.duplicate())) {
                if(!isPassthru()) {
                    m_logger.warn("Declaring passthru on advice of SessionMonitor, yet " +
                                  "should have already been declared by other half of casing");
                }
                declarePassthru();
            }
        }
        return new UnparseResult(buf);
    }

}
