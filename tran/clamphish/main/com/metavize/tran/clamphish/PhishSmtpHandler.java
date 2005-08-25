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

import java.io.*;
import java.util.*;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.*;
import com.metavize.tran.mail.*;
import com.metavize.tran.mail.papi.*;
import com.metavize.tran.mail.papi.smtp.*;
import com.metavize.tran.mail.papi.smtp.sapi.*;
import com.metavize.tran.mime.*;
import com.metavize.tran.spam.SpamSMTPConfig;
import com.metavize.tran.util.*;
import org.apache.log4j.Logger;
import com.metavize.mvvm.tran.Transform;


/**
 * Protocol Handler which is called-back as scanable messages
 * are encountered.
 */
public class PhishSmtpHandler extends com.metavize.tran.spam.SmtpSessionHandler {

    private static final String SPAM_HEADER_NAME = "X-Phish-Flag";
    private static final LCString SPAM_HEADER_NAME_LC = new LCString(SPAM_HEADER_NAME);

    PhishSmtpHandler(TCPSession session,
                     long maxClientWait,
                     long maxSvrWait,
                     ClamPhishTransform impl,
                     SpamSMTPConfig config,
                     WrappedMessageGenerator wrapper,
                     SmtpNotifyMessageGenerator notifier) {
        super(session, maxClientWait, maxSvrWait, impl, config, wrapper, notifier);
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
