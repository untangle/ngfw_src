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

package com.untangle.tran.mail.impl.smtp;

import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.mail.impl.AbstractMailUnparser;
import org.apache.log4j.Logger;

/**
 * Base class for the SmtpClient/ServerUnparser
 */
abstract class SmtpUnparser
    extends AbstractMailUnparser {

    private final Logger m_logger = Logger.getLogger(SmtpUnparser.class);
    private CasingSessionTracker m_tracker;

    protected SmtpUnparser(TCPSession session,
                           SmtpCasing parent,
                           CasingSessionTracker tracker,
                           boolean clientSide) {

        super(session, parent, clientSide, "smtp");
        m_tracker = tracker;
    }

    SmtpCasing getSmtpCasing() {
        return (SmtpCasing) getParentCasing();
    }

    CasingSessionTracker getSessionTracker() {
        return m_tracker;
    }
}
