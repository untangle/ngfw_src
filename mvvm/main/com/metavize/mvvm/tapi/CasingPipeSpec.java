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

package com.metavize.mvvm.tapi;

import java.util.Set;

public class CasingPipeSpec extends PipeSpec
{
    private final Fitting input;
    private final Fitting output;

    // constructors -----------------------------------------------------------

    public CasingPipeSpec(String name, Set subscriptions,
                          Fitting input, Fitting output)
    {
        super(name, subscriptions);
        this.input = input;
        this.output = output;
    }

    // accessors --------------------------------------------------------------

    public Fitting getInput()
    {
        return input;
    }

    public Fitting getOutput()
    {
        return output;
    }
}
