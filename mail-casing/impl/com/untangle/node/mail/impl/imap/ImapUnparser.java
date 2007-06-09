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

package com.untangle.node.mail.impl.imap;

import com.untangle.uvm.tapi.TCPSession;
import com.untangle.node.mail.impl.AbstractMailUnparser;
import org.apache.log4j.Logger;

/**
 * Base class for the ImapClient/ServerUnparser
 */
abstract class ImapUnparser
    extends AbstractMailUnparser {

    //  private final Logger m_logger = Logger.getLogger(ImapUnparser.class);

    protected ImapUnparser(TCPSession session,
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
}
