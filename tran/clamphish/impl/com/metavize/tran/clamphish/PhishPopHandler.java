/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
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
import com.metavize.tran.mail.papi.MailExport;
import com.metavize.tran.spam.SpamPopHandler;

public class PhishPopHandler extends SpamPopHandler
{
    // constructors -----------------------------------------------------------
    PhishPopHandler(TCPSession session, ClamPhishTransform transform, MailExport zMExport)
    {
        super(session, transform, zMExport);
    }
}
