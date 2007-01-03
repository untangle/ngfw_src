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
import com.untangle.tran.mail.papi.safelist.SafelistTransformView;
import com.untangle.tran.spam.SpamImapHandler;
import com.untangle.tran.spam.SpamIMAPConfig;

class PhishImapHandler extends SpamImapHandler
{
    PhishImapHandler(TCPSession session,
                     long maxClientWait,
                     long maxSvrWait,
                     ClamPhishTransform impl,
                     SpamIMAPConfig config,
                     SafelistTransformView safelist) {
        super(session, maxClientWait, maxSvrWait, impl, config, safelist);
    }
}
