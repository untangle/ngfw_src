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

package com.untangle.tran.virus;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// XXX convert to enum when we dump XDoclet

public class SMTPVirusMessageAction implements Serializable
{
    private static final long serialVersionUID = -6364692037092527263L;

    private static final Map INSTANCES = new HashMap();

    public static final char PASS_KEY = 'P';
    public static final char REMOVE_KEY = 'R';
    public static final char BLOCK_KEY = 'B';

    public static final SMTPVirusMessageAction PASS = new SMTPVirusMessageAction(PASS_KEY, "pass message");
    public static final SMTPVirusMessageAction REMOVE = new SMTPVirusMessageAction(REMOVE_KEY, "remove infection");
    public static final SMTPVirusMessageAction BLOCK = new SMTPVirusMessageAction(BLOCK_KEY, "block message");

    static {
        INSTANCES.put(PASS.getKey(), PASS);
        INSTANCES.put(REMOVE.getKey(), REMOVE);
        INSTANCES.put(BLOCK.getKey(), BLOCK);
    }

    private final String name;
    private final char key;

    private SMTPVirusMessageAction(char key, String name)
    {
        this.key = key;
        this.name = name;
    }

    public static SMTPVirusMessageAction getInstance(char key)
    {
        return (SMTPVirusMessageAction)INSTANCES.get(key);
    }

    public static SMTPVirusMessageAction getInstance(String name)
    {
        SMTPVirusMessageAction zMsgAction;
        for (Iterator i = INSTANCES.keySet().iterator(); true == i.hasNext(); )
            {
                zMsgAction = (SMTPVirusMessageAction)INSTANCES.get(i.next());
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

    public static SMTPVirusMessageAction[] getValues()
    {
        SMTPVirusMessageAction[] azMsgAction = new SMTPVirusMessageAction[INSTANCES.size()];
        Iterator iter = INSTANCES.keySet().iterator();
        SMTPVirusMessageAction zMsgAction;
        for (int i = 0; true == iter.hasNext(); i++) {
            zMsgAction = (SMTPVirusMessageAction)INSTANCES.get(iter.next());
            azMsgAction[i] = zMsgAction;
        }
        return azMsgAction;
    }
}
