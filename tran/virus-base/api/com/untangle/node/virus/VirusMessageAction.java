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

package com.untangle.node.virus;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// XXX convert to enum when we dump XDoclet

public class VirusMessageAction implements Serializable
{
    private static final long serialVersionUID = -6364692037092527263L;

    private static final Map INSTANCES = new HashMap();

    public static final char PASS_KEY = 'P';
    public static final char REMOVE_KEY = 'R';

    public static final VirusMessageAction PASS = new VirusMessageAction(PASS_KEY, "pass message");
    public static final VirusMessageAction REMOVE = new VirusMessageAction(REMOVE_KEY, "remove infection");

    static {
        INSTANCES.put(PASS.getKey(), PASS);
        INSTANCES.put(REMOVE.getKey(), REMOVE);
    }

    private final String name;
    private final char key;

    private VirusMessageAction(char key, String name)
    {
        this.key = key;
        this.name = name;
    }

    public static VirusMessageAction getInstance(char key)
    {
        return (VirusMessageAction)INSTANCES.get(key);
    }

    public static VirusMessageAction getInstance(String name)
    {
        VirusMessageAction zMsgAction;
        for (Iterator i = INSTANCES.keySet().iterator(); true == i.hasNext(); )
            {
                zMsgAction = (VirusMessageAction)INSTANCES.get(i.next());
                if (name.equals(zMsgAction.getName())) {
                    return zMsgAction;
                }
            }
        return null;
    }

    public String toString()
    {
        return name;
    }

    public char getKey()
    {
        return key;
    }

    public String getName()
    {
        return name;
    }

    Object readResolve()
    {
        return getInstance(key);
    }

    public static VirusMessageAction[] getValues()
    {
        VirusMessageAction[] azMsgAction = new VirusMessageAction[INSTANCES.size()];
        Iterator iter = INSTANCES.keySet().iterator();
        VirusMessageAction zMsgAction;
        for (int i = 0; true == iter.hasNext(); i++) {
            zMsgAction = (VirusMessageAction)INSTANCES.get(iter.next());
            azMsgAction[i] = zMsgAction;
        }
        return azMsgAction;
    }
}
