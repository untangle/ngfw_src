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

package com.untangle.node.clamphish;

import com.untangle.uvm.tapi.TCPSession;
import com.untangle.node.mail.papi.quarantine.QuarantineNodeView;
import com.untangle.node.mail.papi.safelist.SafelistNodeView;
import com.untangle.node.spam.SpamReport;
import com.untangle.node.spam.SpamSMTPConfig;

/**
 * Protocol Handler which is called-back as scanable messages
 * are encountered.
 */
public class PhishSmtpHandler extends com.untangle.node.spam.SmtpSessionHandler {

    PhishSmtpHandler(TCPSession session,
                     long maxClientWait,
                     long maxSvrWait,
                     ClamPhishNode impl,
                     SpamSMTPConfig config,
                     QuarantineNodeView quarantine,
                     SafelistNodeView safelist) {
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
