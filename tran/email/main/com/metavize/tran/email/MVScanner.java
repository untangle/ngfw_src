/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MVScanner.java,v 1.1 2005/01/22 05:34:24 jdi Exp $
 */
package com.metavize.tran.email;

import java.util.*;
import java.io.*;

public abstract class MVScanner
{
    protected static final int EXPAND_ROOM = 255;

    public abstract byte[] scanEmail (byte[] inbuf, int len) throws IOException,InterruptedException;

    public abstract ArrayList scanEmail (ArrayList bufs) throws IOException,InterruptedException;
}
