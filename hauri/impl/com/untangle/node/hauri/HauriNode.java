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
package com.untangle.node.hauri;

import com.untangle.node.virus.VirusNodeImpl;

public class HauriNode extends VirusNodeImpl
{
    public HauriNode()
    {
        super(new HauriScanner());
    }

    protected int getStrength()
    {
        return 17;
    }

}
