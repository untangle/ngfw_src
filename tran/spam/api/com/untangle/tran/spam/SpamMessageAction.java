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

public class SpamMessageAction implements Serializable
{
    private static final long serialVersionUID = -6364692037092527263L;

    private static final Map INSTANCES = new HashMap();

    public static char PASS_KEY = 'P';
    public static char MARK_KEY = 'M';
    public static char SAFELIST_KEY = 'S'; // special pass case
    public static char OVERSIZE_KEY = 'Z'; // special pass case

    public static final SpamMessageAction PASS = new SpamMessageAction(PASS_KEY, "pass message");
    public static final SpamMessageAction MARK = new SpamMessageAction(MARK_KEY, "mark message");
    // for now, label safelist and oversize messages as pass messages
    // until someone requests more detail
    public static final SpamMessageAction SAFELIST = new SpamMessageAction(SAFELIST_KEY, "pass message");
    public static final SpamMessageAction OVERSIZE = new SpamMessageAction(OVERSIZE_KEY, "pass message");

    static {
        INSTANCES.put(PASS.getKey(), PASS);
        INSTANCES.put(MARK.getKey(), MARK);
        INSTANCES.put(SAFELIST.getKey(), SAFELIST);
        INSTANCES.put(OVERSIZE.getKey(), OVERSIZE);
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
