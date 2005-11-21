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

package com.metavize.tran.spam;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.metavize.tran.mail.papi.smtp.SMTPNotifyAction;

// XXX convert to enum when we dump XDoclet

public class SpamSMTPNotifyAction extends SMTPNotifyAction
{
    private static final long serialVersionUID = -6364692037092527263L;

    private static final Map INSTANCES = new HashMap();

    public static final SpamSMTPNotifyAction SENDER = new SpamSMTPNotifyAction(sndr_c, sndr_s);
    public static final SpamSMTPNotifyAction NEITHER = new SpamSMTPNotifyAction(none_c, none_s);

    static {
        INSTANCES.put(SENDER.getKey(), SENDER);
        INSTANCES.put(NEITHER.getKey(), NEITHER);
    }

    protected SpamSMTPNotifyAction(char key, String name)
    {
        super(key, name);
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
}
