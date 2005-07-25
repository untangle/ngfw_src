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

package com.metavize.tran.spam;

import com.metavize.mvvm.argon.IntfConverter;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.mail.PopStateMachine;
import com.metavize.tran.token.TokenException;
import com.metavize.tran.token.TokenResult;
import org.apache.log4j.Logger;

class SpamPopHandler extends PopStateMachine
{
    private final Logger logger = Logger.getLogger(SpamPopHandler.class);

    private final SpamScanner zScanner;

    private final SpamMessageAction zMsgAction;
    private final boolean bScan;

    // constructors -----------------------------------------------------------

    SpamPopHandler(TCPSession session, SpamImpl transform)
    {
        super(session);

        zScanner = transform.getScanner();

        SpamPOPConfig zConfig;
        if (IntfConverter.INSIDE == session.clientIntf())
        {
            zConfig = transform.getSpamSettings().getPOPInbound();
        }
        else
        {
            zConfig = transform.getSpamSettings().getPOPOutbound();
        }
        bScan = zConfig.getScan();
        zMsgAction = zConfig.getMsgAction();
    }

    // PopStateMachine methods -----------------------------------------------

    protected TokenResult scanMessage() throws TokenException
    {
//XXXX
        return null;
    }
}
