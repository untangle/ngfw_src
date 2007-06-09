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
package com.untangle.node.spamassassin;

import com.untangle.node.spam.SpamImpl;

public class SpamAssassinNode extends SpamImpl
{
    public SpamAssassinNode()
    {
        super(new SpamAssassinScanner());
    }
}
