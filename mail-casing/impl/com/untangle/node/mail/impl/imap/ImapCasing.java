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

import org.apache.log4j.Logger;

import com.untangle.node.mail.impl.AbstractMailCasing;
import com.untangle.node.token.Parser;
import com.untangle.node.token.Unparser;
import com.untangle.uvm.vnet.NodeTCPSession;


/**
 * 'name says it all...
 */
class ImapCasing
    extends AbstractMailCasing {

    private final Logger m_logger =
        Logger.getLogger(ImapCasing.class);

    private final ImapParser m_parser;
    private final ImapUnparser m_unparser;


    private final ImapSessionMonitor m_sessionMonitor;

    ImapCasing(NodeTCPSession session,
               boolean clientSide) {

        super(session, clientSide, "imap");

        //This sillyness is to work around some issues
        //with classloaders and logging
        try {
            new com.untangle.node.mail.papi.imap.CompleteImapMIMEToken(null, null);
        }
        catch(Exception ignore){}

        m_logger.debug("Created");
        m_sessionMonitor = new ImapSessionMonitor();
        m_parser = clientSide? new ImapClientParser(session, this): new ImapServerParser(session, this);
        m_unparser = clientSide? new ImapClientUnparser(session, this): new ImapServerUnparser(session, this);
    }

    /**
     * Get the SessionMonitor for this Casing, which
     * performs read-only examination of the IMAP
     * conversation looking for username, as well
     * as commands like STARTTLS which require
     * passthru
     */
    ImapSessionMonitor getSessionMonitor() {
        return m_sessionMonitor;
    }

    public Parser parser() {
        return m_parser;
    }

    public Unparser unparser() {
        return m_unparser;
    }
}
