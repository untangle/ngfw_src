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
package com.untangle.node.virus;

import java.util.regex.*;

public class VirusPattern {

    protected Pattern pattern = null;
    protected boolean scan = false;

    public VirusPattern (Pattern pat, boolean scan)
    {
        this.pattern = pat;
        this.scan = scan;
    }
}
