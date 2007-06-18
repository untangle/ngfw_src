/*
 * $HeadURL$
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

package com.untangle.node.virus;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// XXX convert to enum when we dump XDoclet

public class SMTPVirusMessageAction implements Serializable
{
    private static final long serialVersionUID = -6364692037092527263L;

    private static final Map INSTANCES = new HashMap();

    public static final char PASS_KEY = 'P';
    public static final char REMOVE_KEY = 'R';
    public static final char BLOCK_KEY = 'B';

    public static final SMTPVirusMessageAction PASS = new SMTPVirusMessageAction(PASS_KEY, "pass message");
    public static final SMTPVirusMessageAction REMOVE = new SMTPVirusMessageAction(REMOVE_KEY, "remove infection");
    public static final SMTPVirusMessageAction BLOCK = new SMTPVirusMessageAction(BLOCK_KEY, "block message");

    static {
        INSTANCES.put(PASS.getKey(), PASS);
        INSTANCES.put(REMOVE.getKey(), REMOVE);
        INSTANCES.put(BLOCK.getKey(), BLOCK);
    }

    private final String name;
    private final char key;

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
