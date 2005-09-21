/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Reason.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.tran.httpblocker;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

// XXX to enum when we XDoclet gets out of the way

public class Action implements Serializable
{
    private static final long serialVersionUID = -1388743204136725990L;

    public static final Action PASS = new Action('P', "pass");
    public static final Action BLOCK = new Action('B', "block");

    private static final Map INSTANCES = new HashMap();

    static {
        INSTANCES.put('P', PASS);
        INSTANCES.put('B', BLOCK);
    }

    private final char key;
    private final String action;

    private Action(char key, String action)
    {
        this.key = key;
        this.action = action;
    }

    public char getKey()
    {
        return key;
    }

    public String toString()
    {
        return action;
    }

    public static Action getInstance(char key)
    {
        return (Action)INSTANCES.get(key);
    }

    // serialization methods --------------------------------------------------

    private Object readResolve()
    {
        return INSTANCES.get(key);
    }
}
