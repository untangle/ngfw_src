/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.spamassassin;

import com.metavize.tran.spam.SpamImpl;

public class SpamAssassinTransform extends SpamImpl
{
    public SpamAssassinTransform()
    {
        super(new SpamAssassinScanner());
    }
}
