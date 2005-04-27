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

public abstract class CasingPipeSpec extends PipeSpec
{
    private final Fitting input;

    // constructors -----------------------------------------------------------

    public CasingPipeSpec(String name, Set subscriptions,
                          Fitting input)
    {
        super(name, subscriptions);
        this.input = input;
    }

    // abstract methods -------------------------------------------------------

    public abstract Fitting getOutput(Fitting input);

    // accessors --------------------------------------------------------------

    public Fitting getInput()
    {
        return input;
    }
}
