/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: VirusPattern.java,v 1.1 2005/01/17 09:04:46 amread Exp $
 */
package com.metavize.tran.virus;

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
