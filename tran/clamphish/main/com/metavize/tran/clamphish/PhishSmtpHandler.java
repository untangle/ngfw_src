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
import com.metavize.tran.spam.SpamSMTPConfig;

/**
 * Protocol Handler which is called-back as scanable messages
 * are encountered.
 */
public class PhishSmtpHandler extends com.metavize.tran.spam.SmtpSessionHandler {

    PhishSmtpHandler(TCPSession session,
                     long maxClientWait,
                     long maxSvrWait,
                     ClamPhishTransform impl,
                     SpamSMTPConfig config) {
        super(session, maxClientWait, maxSvrWait, impl, config);
    }
}
