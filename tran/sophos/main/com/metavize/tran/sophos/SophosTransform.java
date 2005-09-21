/*
 * Copyright (c) 2003, 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.sophos;

import com.metavize.tran.virus.VirusTransformImpl;

public class SophosTransform extends VirusTransformImpl
{
    public SophosTransform()
    {
        super(new SophosScanner());
    }
}
