/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.mail.papi.smtp;

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
