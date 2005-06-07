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

import java.util.Set;

import com.metavize.mvvm.tapi.CasingPipeSpec;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.Subscription;

class SmtpPipeSpec extends CasingPipeSpec
{
    SmtpPipeSpec(Set<Subscription> subscriptions)
    {
        super("smtp", subscriptions, Fitting.SMTP_STREAM);
    }

    // CasingPipeSpec methods -------------------------------------------------

    @Override
    public Fitting getOutput(Fitting input)
    {
        if (Fitting.SMTP_STREAM == input) {
            return Fitting.SMTP_TOKENS;
        } else {
            throw new IllegalStateException("bad input: " + input);
        }
    }
}
