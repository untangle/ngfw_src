/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.clamphish;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.mail.papi.safelist.SafelistTransformView;
import com.metavize.tran.spam.SpamImapHandler;
import com.metavize.tran.spam.SpamIMAPConfig;

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
