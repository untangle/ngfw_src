/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// XXX convert to enum when we dump XDoclet

public class NotifyAction implements Serializable
{
    private static final long serialVersionUID = -6364692037092527263L;

    private static final Map INSTANCES = new HashMap();

    public static final NotifyAction SENDER = new NotifyAction('S', "notify sender");
    public static final NotifyAction RECEIVER = new NotifyAction('R', "notify receiver");
    public static final NotifyAction BOTH = new NotifyAction('B', "notify sender and receiver");
    public static final NotifyAction NEITHER = new NotifyAction('N', "do not notify");

    static {
        INSTANCES.put(SENDER.getKey(), SENDER);
        INSTANCES.put(RECEIVER.getKey(), RECEIVER);
        INSTANCES.put(BOTH.getKey(), BOTH);
        INSTANCES.put(NEITHER.getKey(), NEITHER);
    }

    private String name;
    private char key;

    private NotifyAction(char key, String name)
    {
        this.key = key;
        this.name = name;
    }

    public static NotifyAction getInstance(char key)
    {
        return (NotifyAction)INSTANCES.get(key);
    }

    public static NotifyAction getInstance(String name)
    {
        NotifyAction a;
        for (Iterator i = INSTANCES.keySet().iterator(); true == i.hasNext(); )
        {
            a = (NotifyAction)INSTANCES.get(i.next());
            if (name.equals(a.getName())) {
                return a;
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

    public static NotifyAction[] getValues()
    {
        NotifyAction[] azMsgAction = new NotifyAction[INSTANCES.size()];
        Iterator iter = INSTANCES.keySet().iterator();
        NotifyAction zMsgAction;
        for (int i = 0; true == iter.hasNext(); i++) {
            zMsgAction = (NotifyAction)INSTANCES.get(iter.next());
            azMsgAction[i] = zMsgAction;
        }
        return azMsgAction;
    }
}
