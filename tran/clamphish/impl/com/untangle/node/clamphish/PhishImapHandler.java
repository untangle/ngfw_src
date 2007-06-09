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
import com.untangle.node.mail.papi.safelist.SafelistNodeView;
import com.untangle.node.spam.SpamImapHandler;
import com.untangle.node.spam.SpamIMAPConfig;

class PhishImapHandler extends SpamImapHandler
{
    PhishImapHandler(TCPSession session,
                     long maxClientWait,
                     long maxSvrWait,
                     ClamPhishNode impl,
                     SpamIMAPConfig config,
                     SafelistNodeView safelist) {
        super(session, maxClientWait, maxSvrWait, impl, config, safelist);
    }
}
