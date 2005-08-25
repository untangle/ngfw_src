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

package com.metavize.tran.clamphish;


import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.spam.SpamImapHandler;
import com.metavize.tran.mail.papi.imap.ImapStateMachine;
import org.apache.log4j.Logger;

class PhishImapHandler extends SpamImapHandler
{
    private final Logger logger = Logger.getLogger(PhishImapHandler.class);

    // constructors -----------------------------------------------------------

    PhishImapHandler(TCPSession session, ClamPhishTransform transform)
    {
        super(session, transform);
    }

    // ImapStateMachine methods -----------------------------------------------

}
