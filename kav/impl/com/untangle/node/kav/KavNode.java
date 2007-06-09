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
package com.untangle.node.kav;

import com.untangle.node.virus.VirusNodeImpl;

public class KavNode extends VirusNodeImpl
{
    public KavNode()
    {
        super(new KavScanner());
    }

    protected int getStrength()
    {
        return 18;
    }
}
