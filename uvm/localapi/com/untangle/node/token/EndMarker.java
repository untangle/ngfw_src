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

package com.untangle.tran.token;


/**
 * Marks the end of a set of {@link Chunk}s.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class EndMarker extends MetadataToken
{
    public static final EndMarker MARKER = new EndMarker();

    private EndMarker() { }
}
