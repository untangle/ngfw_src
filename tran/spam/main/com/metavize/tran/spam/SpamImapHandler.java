/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.spam;


import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.mail.papi.imap.ImapStateMachine;
import org.apache.log4j.Logger;

class SpamImapHandler extends ImapStateMachine
{
    private final Logger logger = Logger.getLogger(SpamImapHandler.class);

    private final SpamImpl transform;

    // constructors -----------------------------------------------------------

    SpamImapHandler(TCPSession session, SpamImpl transform)
    {
        super(session);
        this.transform = transform;
    }

    // ImapStateMachine methods -----------------------------------------------

}
