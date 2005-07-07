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

package com.metavize.tran.mail;

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.CasingPipeSpec;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import org.apache.log4j.Logger;

public class MailTransform extends AbstractTransform
{
    private final Logger logger = Logger.getLogger(MailTransform.class);

    private final PipeSpec[] pipeSpecs = new PipeSpec[] {
        new CasingPipeSpec("smtp", this, SmtpCasingFactory.factory(),
                           Fitting.SMTP_STREAM, Fitting.SMTP_TOKENS)
    };

    // constructors -----------------------------------------------------------

    public MailTransform() { }

    // AbstractTransform methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        throw new UnsupportedOperationException("bad move");
    }

    public void setSettings(Object settings)
    {
        throw new UnsupportedOperationException("bad move");
    }
}
