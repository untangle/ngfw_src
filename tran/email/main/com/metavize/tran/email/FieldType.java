/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: FieldType.java,v 1.3 2005/03/11 21:07:03 cng Exp $
 */

package com.metavize.tran.email;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// XXX convert to enum when we dump XDoclet

public class FieldType implements Serializable
{
    private static final long serialVersionUID = -4444096939833912487L;

    private static final Map INSTANCES = new HashMap();

    public static final FieldType CONTENT_TYPE = new FieldType('C', "Content-Type");
    public static final FieldType MIME_CONTENT_TYPE = new FieldType('T', "MIME Content-Type");
    public static final FieldType MIME_CONTENT_ENCODE = new FieldType('E', "MIME Content-Encode");
    public static final FieldType ORIGINATOR = new FieldType('O', "Originator");
    public static final FieldType RECIPIENT = new FieldType('R', "Recipient");
    public static final FieldType RELAY = new FieldType('Y', "Relay");
    public static final FieldType SENDER = new FieldType('F', "Sender");
    public static final FieldType SUBJECT = new FieldType('S', "Subject");
    public static final FieldType NONE = new FieldType('*', ""); /* exclude from HashMap */

    static {
        INSTANCES.put(CONTENT_TYPE.getKey(), CONTENT_TYPE);
        INSTANCES.put(MIME_CONTENT_TYPE.getKey(), MIME_CONTENT_TYPE);
        INSTANCES.put(MIME_CONTENT_ENCODE.getKey(), MIME_CONTENT_ENCODE);
        INSTANCES.put(ORIGINATOR.getKey(), ORIGINATOR);
        INSTANCES.put(RECIPIENT.getKey(), RECIPIENT);
        INSTANCES.put(RELAY.getKey(), RELAY);
        INSTANCES.put(SENDER.getKey(), SENDER);
        INSTANCES.put(SUBJECT.getKey(), SUBJECT);
    }

    private char cKey;
    private String zName;

    private FieldType(char cKey, String zName)
    {
        this.cKey = cKey;
        this.zName = zName;
    }

    public static FieldType getInstance(char cKey)
    {
        return (FieldType)INSTANCES.get(cKey);
    }

    public static FieldType getInstance(String zName)
    {
        FieldType a;
        for (Iterator i = INSTANCES.keySet().iterator(); true == i.hasNext(); )
        {
            a = (FieldType)INSTANCES.get(i.next());
            if (zName.equals(a.getName())) {
                return a;
            }
        }
        return null;
    }

    public String toString()
    {
        return zName;
    }

    public char getKey()
    {
        return cKey;
    }

    public String getName()
    {
        return zName;
    }

    Object readResolve()
    {
        return getInstance(cKey);
    }

    public static FieldType[] values()
    {
        FieldType[] result = new FieldType[INSTANCES.size()];
        Iterator iter = INSTANCES.keySet().iterator();
        FieldType a;
        for (int i = 0; true == iter.hasNext(); i++) {
            a = (FieldType)INSTANCES.get(iter.next());
            result[i] = a;
        }
        return result;
    }
}
