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
package com.untangle.tran.clam;

import com.untangle.tran.virus.VirusTransformImpl;

public class ClamTransform extends VirusTransformImpl
{
    protected int getStrength()
    {
        return 5;
    }

    public ClamTransform()
    {
        super(new ClamScanner());
    }
}
