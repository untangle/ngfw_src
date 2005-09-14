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
import com.metavize.tran.spam.SpamIMAPConfig;
import com.metavize.tran.mime.LCString;
import com.metavize.tran.mail.papi.WrappedMessageGenerator;
import org.apache.log4j.Logger;

class PhishImapHandler extends SpamImapHandler
{
    private static final String SPAM_HEADER_NAME = "X-Phish-Flag";
    private static final LCString SPAM_HEADER_NAME_LC = new LCString(SPAM_HEADER_NAME);

    PhishImapHandler(TCPSession session,
      long maxClientWait,
      long maxSvrWait,
      ClamPhishTransform impl,
      SpamIMAPConfig config,
      WrappedMessageGenerator wrapper) {
      super(session, maxClientWait, maxSvrWait, impl, config, wrapper);
    }

    @Override
    protected String spamHeaderName() {
        return SPAM_HEADER_NAME;
    }
    @Override
    protected LCString spamHeaderNameLC() {
        return SPAM_HEADER_NAME_LC;
    }
}
