/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: HauriTransform.java,v 1.1 2005/02/26 04:11:31 dmorris Exp $
 */
package com.metavize.tran.clam;

import com.metavize.tran.virus.VirusTransformImpl;

public class HauriTransform extends VirusTransformImpl
{
    public HauriTransform()
    {
        super(new HauriScanner());
    }
}
