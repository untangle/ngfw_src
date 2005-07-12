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

public class SMTPVirusMessageAction implements Serializable
{
    private static final long serialVersionUID = -6364692037092527263L;

    private static final Map INSTANCES = new HashMap();

    public static final SMTPVirusMessageAction PASS = new SMTPVirusMessageAction('P', "pass message");
    public static final SMTPVirusMessageAction CLEAN = new SMTPVirusMessageAction('C', "clean message");
    public static final SMTPVirusMessageAction BLOCK = new SMTPVirusMessageAction('B', "block message");

    static {
        INSTANCES.put(PASS.getKey(), PASS);
        INSTANCES.put(CLEAN.getKey(), CLEAN);
        INSTANCES.put(BLOCK.getKey(), BLOCK);
    }

    private String name;
    private char key;

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
