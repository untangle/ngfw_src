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

package com.untangle.mvvm.security;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class LogoutReason implements Serializable
{
    private static final long serialVersionUID = 2003060303088994717L;

    public static final LogoutReason TIMEOUT = new LogoutReason('T', "timed out");
    public static final LogoutReason ADMINISTRATOR = new LogoutReason('A', "administrator initiated");
    public static final LogoutReason USER = new LogoutReason('U', "user initiated");

    private static final Map INSTANCES = new HashMap();

    static {
        INSTANCES.put('T', TIMEOUT);
        INSTANCES.put('A', ADMINISTRATOR);
        INSTANCES.put('U', USER);
    }

    private final char key;
    private final String reason;

    private LogoutReason(char key, String reason)
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

    public static LogoutReason getInstance(char key)
    {
        return (LogoutReason)INSTANCES.get(key);
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof LogoutReason)) {
            return false;
        } else {
            LogoutReason lfr = (LogoutReason)o;
            return key == lfr.key;
        }
    }

    public int hashCode()
    {
        return (int)key;
    }
}
