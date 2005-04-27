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

package com.metavize.tran.http;

import java.util.Set;

import com.metavize.mvvm.tapi.CasingPipeSpec;
import com.metavize.mvvm.tapi.Fitting;

class HttpPipeSpec extends CasingPipeSpec
{
    HttpPipeSpec(Set subscriptions)
    {
        super("http", subscriptions, Fitting.HTTP_STREAM);
    }

    // CasingPipeSpec methods -------------------------------------------------

    @Override
    public Fitting getOutput(Fitting input)
    {
        return Fitting.HTTP_TOKENS;
    }
}
