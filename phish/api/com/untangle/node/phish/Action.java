/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.phish;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

// XXX to enum when we XDoclet gets out of the way

public class Action implements Serializable
{
    private static final long serialVersionUID = -1388743204136835821L;

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
