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

package com.untangle.node.httpblocker;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

// XXX to enum when we XDoclet gets out of the way

public class Action implements Serializable
{
    private static final long serialVersionUID = -1388743204136725990L;

    public static char PASS_KEY = 'P';
    public static char BLOCK_KEY = 'B';

    public static final Action PASS = new Action(PASS_KEY, "pass");
    public static final Action BLOCK = new Action(BLOCK_KEY, "block");

    private static final Map INSTANCES = new HashMap();

    static {
        INSTANCES.put(PASS.getKey(), PASS);
        INSTANCES.put(BLOCK.getKey(), BLOCK);
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
