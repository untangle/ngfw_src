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

package com.untangle.tran.mail.papi.smtp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// XXX convert to enum when we dump XDoclet

public class SMTPNotifyAction implements Serializable
{
    private static final long serialVersionUID = -6364692037092527263L;

    private static final Map INSTANCES = new HashMap();

    protected static final char sndr_c = 'S';
    protected static final char rcvr_c = 'R';
    protected static final char both_c = 'B';
    protected static final char none_c = 'N';
    protected static final String sndr_s = "notify sender";
    protected static final String rcvr_s = "notify receiver";
    protected static final String both_s = "notify sender and receiver";
    protected static final String none_s = "do not notify";

    public static final SMTPNotifyAction SENDER = new SMTPNotifyAction(sndr_c, sndr_s);
    public static final SMTPNotifyAction RECEIVER = new SMTPNotifyAction(rcvr_c, rcvr_s);
    public static final SMTPNotifyAction BOTH = new SMTPNotifyAction(both_c, both_s);
    public static final SMTPNotifyAction NEITHER = new SMTPNotifyAction(none_c, none_s);

    static {
        INSTANCES.put(SENDER.getKey(), SENDER);
        INSTANCES.put(RECEIVER.getKey(), RECEIVER);
        INSTANCES.put(BOTH.getKey(), BOTH);
        INSTANCES.put(NEITHER.getKey(), NEITHER);
    }

    private final String name;
    private final char key;

    protected SMTPNotifyAction(char key, String name)
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
