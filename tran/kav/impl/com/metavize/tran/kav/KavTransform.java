/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.kav;

import com.metavize.tran.virus.VirusTransformImpl;

public class KavTransform extends VirusTransformImpl
{
    public KavTransform()
    {
        super(new KavScanner());
    }

    protected int getStrength()
    {
        return 8;
    }
}
