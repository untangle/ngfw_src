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
package com.untangle.node.clam;

import com.untangle.node.virus.VirusNodeImpl;

public class ClamNode extends VirusNodeImpl
{
    protected int getStrength()
    {
        return 15;
    }

    public ClamNode()
    {
        super(new ClamScanner());
    }
}
