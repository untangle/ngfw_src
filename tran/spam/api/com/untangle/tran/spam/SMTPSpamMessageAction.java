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

package com.untangle.tran.spam;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// XXX convert to enum when we dump XDoclet

public class SMTPSpamMessageAction implements Serializable
{
    private static final long serialVersionUID = -6364692037092527263L;

    private static final Map INSTANCES = new HashMap();

    public static char PASS_KEY = 'P';
    public static char MARK_KEY = 'M';
    public static char BLOCK_KEY = 'B';
    public static char QUARANTINE_KEY = 'Q';
    public static char SAFELIST_KEY = 'S'; // special pass case
    public static char OVERSIZE_KEY = 'Z'; // special pass case

    public static final SMTPSpamMessageAction PASS = new SMTPSpamMessageAction(PASS_KEY, "pass message");
    public static final SMTPSpamMessageAction MARK = new SMTPSpamMessageAction(MARK_KEY, "mark message");
    public static final SMTPSpamMessageAction BLOCK = new SMTPSpamMessageAction(BLOCK_KEY, "block message");
    public static final SMTPSpamMessageAction QUARANTINE = new SMTPSpamMessageAction(QUARANTINE_KEY, "quarantine message");
    // for now, label safelist and oversize messages as pass messages
    // until someone requests more detail
    public static final SMTPSpamMessageAction SAFELIST = new SMTPSpamMessageAction(SAFELIST_KEY, "pass message");
    public static final SMTPSpamMessageAction OVERSIZE = new SMTPSpamMessageAction(OVERSIZE_KEY, "pass message");

    static {
        INSTANCES.put(PASS.getKey(), PASS);
        INSTANCES.put(MARK.getKey(), MARK);
        INSTANCES.put(BLOCK.getKey(), BLOCK);
        INSTANCES.put(QUARANTINE.getKey(), QUARANTINE);
        INSTANCES.put(SAFELIST.getKey(), SAFELIST);
        INSTANCES.put(OVERSIZE.getKey(), OVERSIZE);
    }

    private String name;
    private char key;

    private SMTPSpamMessageAction(char key, String name)
    {
        this.key = key;
        this.name = name;
    }

    public static SMTPSpamMessageAction getInstance(char key)
    {
        return (SMTPSpamMessageAction)INSTANCES.get(key);
    }

    public static SMTPSpamMessageAction getInstance(String name)
    {
        SMTPSpamMessageAction zMsgAction;
        for (Iterator i = INSTANCES.keySet().iterator(); true == i.hasNext(); )
        {
            zMsgAction = (SMTPSpamMessageAction)INSTANCES.get(i.next());
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

    public static SMTPSpamMessageAction[] getValues()
    {
        SMTPSpamMessageAction[] azMsgAction = new SMTPSpamMessageAction[INSTANCES.size()];
        Iterator iter = INSTANCES.keySet().iterator();
        SMTPSpamMessageAction zMsgAction;
        for (int i = 0; true == iter.hasNext(); i++) {
            zMsgAction = (SMTPSpamMessageAction)INSTANCES.get(iter.next());
            azMsgAction[i] = zMsgAction;
        }
        return azMsgAction;
    }
}
