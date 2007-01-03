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

package com.untangle.tran.portal.vnc;

import com.untangle.mvvm.portal.*;
import java.util.StringTokenizer;

public class VncBookmark extends Bookmark
{
    public static final int DISPLAY_NUMBER_DEFAULT = 0;

    public VncBookmark(Bookmark parent)
    {
        setId(parent.getId());
        setName(parent.getName());
        setApplicationName(parent.getApplicationName());
        setTarget(parent.getTarget());
    }

    public String getHost() {
        String target = getTarget();
        if (target == null)
            return null;
        StringTokenizer tok = new StringTokenizer(target, ":");
        if (!tok.hasMoreTokens())
            return null;
        return tok.nextToken();
    }

    public void setHost(String host) {
        int dnum = getDisplayNumber();
        setTarget(host + ":" + dnum);
    }

    public int getDisplayNumber() {
        String target = getTarget();
        if (target == null)
            return DISPLAY_NUMBER_DEFAULT;
        StringTokenizer tok = new StringTokenizer(target, ":");
        if (!tok.hasMoreTokens())
            return DISPLAY_NUMBER_DEFAULT;
        tok.nextToken();
        if (!tok.hasMoreTokens())
            return DISPLAY_NUMBER_DEFAULT;
        String encnum = tok.nextToken();
        try {
            return Integer.parseInt(encnum);
        } catch (NumberFormatException x) {
            return DISPLAY_NUMBER_DEFAULT;
        }
    }

    public void setDisplayNumber(int dnum) {
        String host = getHost();
        setTarget(host + ":" + dnum);
    }
}
