/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.tran.kav;

import com.untangle.tran.virus.VirusTransformImpl;

public class KavTransform extends VirusTransformImpl
{
    public KavTransform()
    {
        super(new KavScanner());
    }

    protected int getStrength()
    {
        return 18;
    }
}
