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

public class MessageAction implements Serializable
{
    private static final long serialVersionUID = -6364692037092527263L;

    private static final Map INSTANCES = new HashMap();

    /* pass and mark are equivalent */
    public static final MessageAction PASS = new MessageAction('P', "pass message");
    public static final MessageAction MARK = new MessageAction('M', "mark message");
    public static final MessageAction BLOCK = new MessageAction('B', "block message");

    static {
        INSTANCES.put(PASS.getKey(), PASS);
        INSTANCES.put(MARK.getKey(), MARK);
        INSTANCES.put(BLOCK.getKey(), BLOCK);
    }

    private String name;
    private char key;

    private MessageAction(char key, String name)
    {
        this.key = key;
        this.name = name;
    }

    public static MessageAction getInstance(char key)
    {
        return (MessageAction)INSTANCES.get(key);
    }

    public static MessageAction getInstance(String name)
    {
        MessageAction a;
        for (Iterator i = INSTANCES.keySet().iterator(); true == i.hasNext(); )
        {
            a = (MessageAction)INSTANCES.get(i.next());
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

    private static MessageAction[] getValues()
    {
        MessageAction[] azMsgAction = new MessageAction[INSTANCES.size()];
        Iterator iter = INSTANCES.keySet().iterator();
        MessageAction zMsgAction;
        for (int i = 0; true == iter.hasNext(); i++) {
            zMsgAction = (MessageAction)INSTANCES.get(iter.next());
            azMsgAction[i] = zMsgAction;
        }
        return azMsgAction;
    }

    public static MessageAction[] getSMTPValues()
    {
        return getValues();
    }

    public static MessageAction[] getPOPValues()
    {
        MessageAction[] azMsgAction = new MessageAction[1 + 1];
        azMsgAction[0] = PASS;
        azMsgAction[1] = MARK;
        return azMsgAction;
    }

    public static MessageAction[] getIMAPValues()
    {
        return getPOPValues();
    }
}
