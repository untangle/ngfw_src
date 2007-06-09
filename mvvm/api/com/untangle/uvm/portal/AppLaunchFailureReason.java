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

package com.untangle.mvvm.portal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class AppLaunchFailureReason implements Serializable
{
    private static final long serialVersionUID = 4003006303098978417L;

    public static final AppLaunchFailureReason NO_DESTINATION = new AppLaunchFailureReason('N', "destination unknown");

    public static final AppLaunchFailureReason LAUNCH_FAILURE = new AppLaunchFailureReason('L', "application error");

    // For Future use:
    public static final AppLaunchFailureReason DISABLED = new AppLaunchFailureReason('D', "disabled by administrator");

    private static final Map INSTANCES = new HashMap();

    static {
        INSTANCES.put('N', NO_DESTINATION);
        INSTANCES.put('L', LAUNCH_FAILURE);
        INSTANCES.put('D', DISABLED);
    }

    private final char key;
    private final String reason;

    private AppLaunchFailureReason(char key, String reason)
    {
        this.key = key;
        this.reason = reason;
    }

    public char getKey()
    {
        return key;
    }

    public String toString()
    {
        return reason;
    }

    private Object readResolve()
    {
        return INSTANCES.get(key);
    }

    public static AppLaunchFailureReason getInstance(char key)
    {
        return (AppLaunchFailureReason)INSTANCES.get(key);
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof AppLaunchFailureReason)) {
            return false;
        } else {
            AppLaunchFailureReason lfr = (AppLaunchFailureReason)o;
            return key == lfr.key;
        }
    }

    public int hashCode()
    {
        return (int)key;
    }
}
