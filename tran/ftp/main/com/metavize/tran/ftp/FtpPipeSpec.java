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

package com.metavize.tran.ftp;

import java.util.Set;

import com.metavize.mvvm.tapi.CasingPipeSpec;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.Subscription;

class FtpPipeSpec extends CasingPipeSpec
{
    FtpPipeSpec(Set<Subscription> subscriptions)
    {
        super("ftp", subscriptions, Fitting.FTP_STREAM);
    }

    // CasingPipeSpec methods -------------------------------------------------

    @Override
    public Fitting getOutput(Fitting input)
    {
        if (Fitting.FTP_CTL_STREAM == input) {
            return Fitting.FTP_CTL_TOKENS;
        } else if (Fitting.FTP_DATA_STREAM == input) {
            return Fitting.FTP_DATA_TOKENS;
        } else {
            throw new IllegalStateException("bad input" + input);
        }
    }
}
