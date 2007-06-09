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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.untangle.tran.mail.papi.smtp.SMTPNotifyAction;

// XXX convert to enum when we dump XDoclet

public class SpamSMTPNotifyAction// extends SMTPNotifyAction
    implements java.io.Serializable {

    private static final long serialVersionUID = -6364692037092527263L;

    private static final Map INSTANCES = new HashMap();

    protected static final char sndr_c = 'S';
    protected static final char none_c = 'N';
    protected static final String sndr_s = "notify sender";
    protected static final String none_s = "do not notify";

    public static final SpamSMTPNotifyAction SENDER = new SpamSMTPNotifyAction(sndr_c, sndr_s);
    public static final SpamSMTPNotifyAction NEITHER = new SpamSMTPNotifyAction(none_c, none_s);

    static {
        INSTANCES.put(SENDER.getKey(), SENDER);
        INSTANCES.put(NEITHER.getKey(), NEITHER);
    }

    private final String name;
    private final char key;

    protected SpamSMTPNotifyAction(char key, String name)
    {
        this.key = key;
        this.name = name;
    }

    public static SpamSMTPNotifyAction getInstance(char key)
    {
        return (SpamSMTPNotifyAction)INSTANCES.get(key);
    }

    public static SpamSMTPNotifyAction getInstance(String name)
    {
        SpamSMTPNotifyAction a;
        for (Iterator i = INSTANCES.keySet().iterator(); true == i.hasNext(); )
            {
                a = (SpamSMTPNotifyAction)INSTANCES.get(i.next());
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


    public static SpamSMTPNotifyAction[] getValues()
    {
        SpamSMTPNotifyAction[] azNotifyAction = new SpamSMTPNotifyAction[INSTANCES.size()];
        Iterator iter = INSTANCES.keySet().iterator();
        SpamSMTPNotifyAction zNotifyAction;
        for (int i = 0; true == iter.hasNext(); i++) {
            zNotifyAction = (SpamSMTPNotifyAction)INSTANCES.get(iter.next());
            azNotifyAction[i] = zNotifyAction;
        }
        return azNotifyAction;
    }

    public static SMTPNotifyAction toSMTPNotifyAction(SpamSMTPNotifyAction action) {
        if(action == SENDER) {
            return SMTPNotifyAction.SENDER;
        }
        if(action == NEITHER) {
            return SMTPNotifyAction.NEITHER;
        }
        return null;
    }
}
