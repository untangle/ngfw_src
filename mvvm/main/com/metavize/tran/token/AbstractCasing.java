/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: AbstractCasing.java,v 1.2 2005/01/25 02:03:34 amread Exp $
 */

package com.metavize.tran.token;

import org.apache.log4j.Logger;

public abstract class AbstractCasing implements Casing
{
    private final Logger logger = Logger.getLogger(AbstractCasing.class);

    public abstract Parser parser();
    public abstract Unparser unparser();
}
