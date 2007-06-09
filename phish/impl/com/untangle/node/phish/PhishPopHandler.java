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

package com.untangle.node.phish;

import com.untangle.uvm.tapi.TCPSession;
import com.untangle.node.mail.papi.MailExport;
import com.untangle.node.spam.SpamPopHandler;

public class PhishPopHandler extends SpamPopHandler
{
    // constructors -----------------------------------------------------------
    PhishPopHandler(TCPSession session, PhishNode node, MailExport zMExport)
    {
        super(session, node, zMExport);
    }
}
