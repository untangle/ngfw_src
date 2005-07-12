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

public class SpamMessageAction implements Serializable
{
    private static final long serialVersionUID = -6364692037092527263L;

    private static final Map INSTANCES = new HashMap();

    public static final SpamMessageAction PASS = new SpamMessageAction('P', "pass message");
    public static final SpamMessageAction MARK = new SpamMessageAction('M', "mark message");

    static {
        INSTANCES.put(PASS.getKey(), PASS);
        INSTANCES.put(MARK.getKey(), MARK);
    }

    private String name;
    private char key;

    private SpamMessageAction(char key, String name)
    {
        this.key = key;
        this.name = name;
    }

    public static SpamMessageAction getInstance(char key)
    {
        return (SpamMessageAction)INSTANCES.get(key);
    }

    public static SpamMessageAction getInstance(String name)
    {
        SpamMessageAction zMsgAction;
        for (Iterator i = INSTANCES.keySet().iterator(); true == i.hasNext(); )
        {
            zMsgAction = (SpamMessageAction)INSTANCES.get(i.next());
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

    public static SpamMessageAction[] getValues()
    {
        SpamMessageAction[] azMsgAction = new SpamMessageAction[INSTANCES.size()];
        Iterator iter = INSTANCES.keySet().iterator();
        SpamMessageAction zMsgAction;
        for (int i = 0; true == iter.hasNext(); i++) {
            zMsgAction = (SpamMessageAction)INSTANCES.get(iter.next());
            azMsgAction[i] = zMsgAction;
        }
        return azMsgAction;
    }
}
