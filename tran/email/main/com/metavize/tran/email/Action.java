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

package com.metavize.tran.email;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// XXX convert to enum when we dump XDoclet

public class Action implements Serializable
{
    private static final long serialVersionUID = -6364692037092527263L;

    private static final Map INSTANCES = new HashMap();

    /* pass and mark are equivalent */
    public static final Action PASS = new Action('P', "pass message");
    public static final Action MARK = new Action('M', "mark message");
    public static final Action BLOCK = new Action('B', "block message");
    public static final Action REPLACE = new Action('C', "replace attachment");
    public static final Action EXCHANGE = new Action('E', "exchange value");
    public static final Action BLOCK_AND_WARN_SENDER = new Action('S', "block, warn sender");
    public static final Action BLOCK_AND_WARN_RECEIVER = new Action('R', "block, warn receiver");
    public static final Action BLOCK_AND_WARN_BOTH = new Action('A', "block, warn sender & receiver");

    static {
        INSTANCES.put(PASS.getKey(), PASS);
        INSTANCES.put(MARK.getKey(), MARK);
        INSTANCES.put(BLOCK.getKey(), BLOCK);
        INSTANCES.put(REPLACE.getKey(), REPLACE);
        INSTANCES.put(EXCHANGE.getKey(), EXCHANGE);
        INSTANCES.put(BLOCK_AND_WARN_SENDER.getKey(), BLOCK_AND_WARN_SENDER);
        INSTANCES.put(BLOCK_AND_WARN_RECEIVER.getKey(), BLOCK_AND_WARN_RECEIVER);
        INSTANCES.put(BLOCK_AND_WARN_BOTH.getKey(), BLOCK_AND_WARN_BOTH);
    }

    private static final int CUSTOM_ACTION_CT = 3;
    private static final int SPAM_ACTION_CT = 5;
    private static final int VIRUS_ACTION_CT = 6;

    private String name;
    private char key;

    private Action(char key, String name)
    {
        this.key = key;
        this.name = name;
    }

    public static Action getInstance(char key)
    {
        return (Action)INSTANCES.get(key);
    }

    public static Action getInstance(String name)
    {
        Action a;
        for (Iterator i = INSTANCES.keySet().iterator(); true == i.hasNext(); )
        {
            a = (Action)INSTANCES.get(i.next());
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

    public static Action[] values()
    {
        Action[] result = new Action[INSTANCES.size()];
        Iterator iter = INSTANCES.keySet().iterator();
        Action a;
        for (int i = 0; true == iter.hasNext(); i++) {
            a = (Action)INSTANCES.get(iter.next());
            result[i] = a;
        }
        return result;
    }

    public static Action[] customValues()
    {
        Action[] result = new Action[CUSTOM_ACTION_CT];
        result[0] = PASS;
        result[1] = BLOCK;
        result[2] = EXCHANGE;
        return result;
    }

    public static Action[] spamValues()
    {
        Action[] result = new Action[SPAM_ACTION_CT];
        result[0] = MARK;
        result[1] = BLOCK;
        result[2] = BLOCK_AND_WARN_SENDER;
        result[3] = BLOCK_AND_WARN_RECEIVER;
        result[4] = BLOCK_AND_WARN_BOTH;
        return result;
    }

    public static Action[] virusValues()
    {
        Action[] result = new Action[VIRUS_ACTION_CT];
        result[0] = PASS;
        result[1] = BLOCK;
        result[2] = REPLACE;
        result[3] = BLOCK_AND_WARN_SENDER;
        result[4] = BLOCK_AND_WARN_RECEIVER;
        result[5] = BLOCK_AND_WARN_BOTH;
        return result;
    }

    // Serializable methods ---------------------------------------------------

    Object reasResolve()
    {
        return getInstance(key);
    }
}
