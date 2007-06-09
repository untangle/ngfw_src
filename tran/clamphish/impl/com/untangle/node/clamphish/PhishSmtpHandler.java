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

package com.untangle.tran.clamphish;

import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.mail.papi.quarantine.QuarantineTransformView;
import com.untangle.tran.mail.papi.safelist.SafelistTransformView;
import com.untangle.tran.spam.SpamReport;
import com.untangle.tran.spam.SpamSMTPConfig;

/**
 * Protocol Handler which is called-back as scanable messages
 * are encountered.
 */
public class PhishSmtpHandler extends com.untangle.tran.spam.SmtpSessionHandler {

    PhishSmtpHandler(TCPSession session,
                     long maxClientWait,
                     long maxSvrWait,
                     ClamPhishTransform impl,
                     SpamSMTPConfig config,
                     QuarantineTransformView quarantine,
                     SafelistTransformView safelist) {
        super(session, maxClientWait, maxSvrWait, impl, config, quarantine, safelist);
    }

    @Override
    protected String getQuarantineCategory() {
        return "FRAUD";
    }

    @Override
    protected String getQuarantineDetail(SpamReport report) {
        //TODO bscott Do something real here
        return "Identity Theft";
    }
}
