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
import com.untangle.tran.mail.papi.MailExport;
import com.untangle.tran.spam.SpamPopHandler;

public class PhishPopHandler extends SpamPopHandler
{
    // constructors -----------------------------------------------------------
    PhishPopHandler(TCPSession session, ClamPhishTransform transform, MailExport zMExport)
    {
        super(session, transform, zMExport);
    }
}
