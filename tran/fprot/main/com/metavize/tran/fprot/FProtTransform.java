/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: FProtTransform.java,v 1.1 2005/01/22 06:06:25 dmorris Exp $
 */
package com.metavize.tran.fprot;

import com.metavize.tran.virus.VirusTransformImpl;

public class FProtTransform extends VirusTransformImpl
{
    public FProtTransform()
    {
        super(new FProtScanner());
    }
}
