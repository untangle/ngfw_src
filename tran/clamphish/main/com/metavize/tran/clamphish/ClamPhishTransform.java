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

import com.metavize.tran.spam.SpamImpl;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.tran.token.TokenAdaptor;
import org.apache.log4j.Logger;

public class ClamPhishTransform extends SpamImpl
{
    // We want to make sure that phish is after spam, before virus in the pipeline (towards the client for smtp,
    // server for pop/imap).
    private final PipeSpec[] pipeSpecs = new PipeSpec[] {
        new SoloPipeSpec("spam-smtp", this, new TokenAdaptor(new PhishSmtpFactory(this)), Fitting.SMTP_TOKENS, Affinity.CLIENT, 8),
        new SoloPipeSpec("pop-smtp", this, new TokenAdaptor(new PhishPopFactory(this)), Fitting.POP_TOKENS, Affinity.SERVER, 8),
        new SoloPipeSpec("imap-smtp", this, new TokenAdaptor(new PhishImapFactory(this)), Fitting.IMAP_TOKENS, Affinity.SERVER, 8)
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

}
