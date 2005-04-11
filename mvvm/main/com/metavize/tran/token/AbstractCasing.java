/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.token;

import org.apache.log4j.Logger;

public abstract class AbstractCasing implements Casing
{
    private final Logger logger = Logger.getLogger(AbstractCasing.class);

    public abstract Tokenizer tokenizer();
    public abstract Untokenizer untokenizer();
}
