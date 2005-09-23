/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
p * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.clamphish;

import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.tran.spam.SpamImpl;
import com.metavize.tran.token.TokenAdaptor;
import static com.metavize.tran.util.Ascii.CRLF;

public class ClamPhishTransform extends SpamImpl
{

    private static final String OUT_MOD_SUB_TEMPLATE =
      "[FRAUD] $MIMEMessage:SUBJECT$";
    private static final String OUT_MOD_BODY_TEMPLATE =
      "The attached message from $MIMEMessage:FROM$ was determined\r\n " +
      "to be PHISH (a fraudulent email intended to steal information)\r\n." +
      "The details of the report are as follows:\r\n\r\n" +
      "$SPAMReport:FULL$";
  
    private static final String IN_MOD_SUB_TEMPLATE = OUT_MOD_SUB_TEMPLATE;
    private static final String IN_MOD_BODY_TEMPLATE = OUT_MOD_BODY_TEMPLATE;

    private static final String OUT_MOD_BODY_SMTP_TEMPLATE =
      "The attached message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$) was determined\r\n " +
      "to be PHISH (a fraudulent email intended to steal information)\r\n." +
      "The details of the report are as follows:\r\n\r\n" +
      "$SPAMReport:FULL$";

    private static final String IN_MOD_BODY_SMTP_TEMPLATE = OUT_MOD_BODY_SMTP_TEMPLATE;

    private static final String PHISH_HEADER_NAME = "X-Phish-Flag";

    private static final String OUT_NOTIFY_SUB_TEMPLATE =
      "[FRAUD NOTIFICATION] re: $MIMEMessage:SUBJECT$";
  
    private static final String OUT_NOTIFY_BODY_TEMPLATE =
        "On $MIMEHeader:DATE$ a message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$) was received " + CRLF +
      "and determined to be PHISH.  The details of the report are as follows:" + CRLF + CRLF +
      "$SPAMReport:FULL$";
  
    private static final String IN_NOTIFY_SUB_TEMPLATE = OUT_NOTIFY_SUB_TEMPLATE;
    private static final String IN_NOTIFY_BODY_TEMPLATE = OUT_NOTIFY_BODY_TEMPLATE;    

    // We want to make sure that phish is after spam, before virus in the pipeline (towards the client for smtp,
    // server for pop/imap).
    private final PipeSpec[] pipeSpecs = new PipeSpec[] {
        new SoloPipeSpec("phish-smtp", this, new TokenAdaptor(this, new PhishSmtpFactory(this)), Fitting.SMTP_TOKENS, Affinity.CLIENT, 8),
        new SoloPipeSpec("phish-pop", this, new TokenAdaptor(this, new PhishPopFactory(this)), Fitting.POP_TOKENS, Affinity.SERVER, 8),
        new SoloPipeSpec("phish-imap", this, new TokenAdaptor(this, new PhishImapFactory(this)), Fitting.IMAP_TOKENS, Affinity.SERVER, 8)
    };

    public ClamPhishTransform()
    {
        super(new ClamPhishScanner());
    }

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    @Override
    public String getDefaultSubjectWrapperTemplate(boolean inbound) {
      return inbound?IN_MOD_SUB_TEMPLATE:OUT_MOD_SUB_TEMPLATE;
    }
    
    @Override
    public String getDefaultBodyWrapperTemplate(boolean inbound) {
      return inbound?IN_MOD_BODY_TEMPLATE:OUT_MOD_BODY_TEMPLATE;
    }

    @Override
    public String getDefaultSMTPSubjectWrapperTemplate(boolean inbound) {
      return getDefaultSubjectWrapperTemplate(inbound);
    }

    @Override
    public String getDefaultSMTPBodyWrapperTemplate(boolean inbound) {
      return inbound?IN_MOD_BODY_SMTP_TEMPLATE:OUT_MOD_BODY_SMTP_TEMPLATE;
    }

    @Override
    public String getDefaultIndicatorHeaderName() {
      return PHISH_HEADER_NAME;
    }

    @Override
    public String getDefaultNotifySubjectTemplate(boolean inbound) {
      return inbound?IN_NOTIFY_SUB_TEMPLATE:OUT_NOTIFY_SUB_TEMPLATE;
    }

    @Override
    public String getDefaultNotifyBodyTemplate(boolean inbound) {
      return inbound?IN_NOTIFY_BODY_TEMPLATE:OUT_NOTIFY_BODY_TEMPLATE;
    }     

}
