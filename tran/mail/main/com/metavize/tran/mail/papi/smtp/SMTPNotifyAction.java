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

package com.metavize.tran.mail.papi.smtp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// XXX convert to enum when we dump XDoclet

public class SMTPNotifyAction implements Serializable
{
    private static final long serialVersionUID = -6364692037092527263L;

    private static final Map INSTANCES = new HashMap();

    public static final SMTPNotifyAction SENDER = new SMTPNotifyAction('S', "notify sender");
    public static final SMTPNotifyAction RECEIVER = new SMTPNotifyAction('R', "notify receiver");
    public static final SMTPNotifyAction BOTH = new SMTPNotifyAction('B', "notify sender and receiver");
    public static final SMTPNotifyAction NEITHER = new SMTPNotifyAction('N', "do not notify");

    static {
        INSTANCES.put(SENDER.getKey(), SENDER);
        INSTANCES.put(RECEIVER.getKey(), RECEIVER);
        INSTANCES.put(BOTH.getKey(), BOTH);
        INSTANCES.put(NEITHER.getKey(), NEITHER);
    }

    private String name;
    private char key;

    private SMTPNotifyAction(char key, String name)
    {
        this.key = key;
        this.name = name;
    }

    public static SMTPNotifyAction getInstance(char key)
    {
        return (SMTPNotifyAction)INSTANCES.get(key);
    }

    public static SMTPNotifyAction getInstance(String name)
    {
        SMTPNotifyAction a;
        for (Iterator i = INSTANCES.keySet().iterator(); true == i.hasNext(); )
        {
            a = (SMTPNotifyAction)INSTANCES.get(i.next());
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

    public static SMTPNotifyAction[] getValues()
    {
        SMTPNotifyAction[] azMsgAction = new SMTPNotifyAction[INSTANCES.size()];
        Iterator iter = INSTANCES.keySet().iterator();
        SMTPNotifyAction zMsgAction;
        for (int i = 0; true == iter.hasNext(); i++) {
            zMsgAction = (SMTPNotifyAction)INSTANCES.get(iter.next());
            azMsgAction[i] = zMsgAction;
        }
        return azMsgAction;
    }
}
