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

package com.metavize.tran.clamphish;

import java.io.File;
import java.io.IOException;

import com.metavize.mvvm.argon.IntfConverter;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.mail.papi.MailExport;
import com.metavize.tran.mail.papi.MailTransformSettings;
import com.metavize.tran.mail.papi.MIMEMessageT;
import com.metavize.tran.mail.papi.WrappedMessageGenerator;
import com.metavize.tran.mail.papi.pop.PopStateMachine;
import com.metavize.tran.mime.HeaderParseException;
import com.metavize.tran.mime.LCString;
import com.metavize.tran.mime.MIMEMessage;
import com.metavize.tran.mime.MIMEUtil;
import com.metavize.tran.spam.SpamPopHandler;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenException;
import com.metavize.tran.token.TokenResult;
import com.metavize.tran.util.FileFactory;
import com.metavize.tran.util.TempFileFactory;
import org.apache.log4j.Logger;

public class PhishPopHandler extends SpamPopHandler
{
    private final static Logger logger = Logger.getLogger(PhishPopHandler.class);
    private final static Logger eventLogger = MvvmContextFactory.context().eventLogger();

    private final static String SPAM_HDR_NAME = "X-Phish-Flag";
    private final static LCString SPAM_HDR_NAME_LC = new LCString(SPAM_HDR_NAME);

    // constructors -----------------------------------------------------------

    PhishPopHandler(TCPSession session, ClamPhishTransform transform, MailExport zMExport)
    {
        super(session, transform, zMExport);

        WrappedMessageGenerator zWMGenerator;

        if (!session.isInbound()) {
            zWMGenerator = new WrappedMessageGenerator(PhishSmtpFactory.IN_MOD_SUB_TEMPLATE, PhishSmtpFactory.IN_MOD_BODY_TEMPLATE);
        } else {
            zWMGenerator = new WrappedMessageGenerator(PhishSmtpFactory.OUT_MOD_SUB_TEMPLATE, PhishSmtpFactory.OUT_MOD_BODY_TEMPLATE);
        }
        zWMsgGenerator = zWMGenerator;
    }

    @Override
    protected String spamHeaderName() {
        return SPAM_HDR_NAME;
    }
    @Override
    protected LCString spamHeaderNameLC() {
        return SPAM_HDR_NAME_LC;
    }
}
