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

package com.untangle.node.token;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Holds an RFC 822 header, as used by HTTP and SMTP.
 *
 * XXX add support for multiple keys of the same name.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class Header implements Token
{
    private Map<String, Field> header = new LinkedHashMap<String, Field>();

    private int estimatedSize = 0;

    public Header() { }

    public void addField(String key, String value)
    {
        Field f = header.get(key.toUpperCase());

        if (null == f) {
            f = new Field(key);
            header.put(key.toUpperCase(), f);
        } else {
            estimatedSize -= f.getEstimatedSize();
        }

        f.addValue(value);
        estimatedSize += f.getEstimatedSize();
    }

    public void removeField(String key)
    {
        Field f = header.remove(key.toUpperCase());

        if (null != f) {
            estimatedSize -= f.getEstimatedSize();
        }
    }

    /**
     * Replace a field value.  If any values exists, they are all
     * removed and the new value is added.
     */
    public void replaceField(String key, String value)
    {
        key = key.toUpperCase();
        Field f = header.get(key);

        /* Item is not in the current header, add a new field */
        if (null == f) {
            f = new Field(key);
            header.put(key, f);
        } else {
            estimatedSize -= f.getEstimatedSize();
            /* Remove all of the items */
            f.values.clear();
        }

        f.addValue( value );
        estimatedSize += f.getEstimatedSize();
    }

    public String getValue(String key)
    {
        Field f = header.get(key.toUpperCase());
        return (null == f || f.values.size() == 0) ? null : f.values.get(0);
    }

    public List getValues(String key)
    {
        Field f = header.get(key.toUpperCase());
        return ( null == f ) ? null : f.values;
    }

    public Iterator<String> keyIterator()
    {
        return new Iterator()
            {
                private Iterator<String> i = header.keySet().iterator();

                public boolean hasNext() {
                    return i.hasNext();
                }

                public String next()
                {
                    Object k = i.next();
                    Field f = header.get(k);
                    return f.key;
                }

                public void remove()
                {
                    i.remove();
                }
            };
    }

    private static class Field
    {
        String key;

        private List<String> values = new LinkedList<String>();
        private int estimatedSize;

        Field(String key)
        {
            this.key = key;
            estimatedSize = key.length();
        }

        Field(String key, String value)
        {
            this.key = key;
            values.add(value);

            estimatedSize = key.length() + value.length();
        }

        void addValue(String value)
        {
            values.add(value);
            estimatedSize += value.length();
        }

        int getEstimatedSize()
        {
            return estimatedSize;
        }
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        StringBuilder sb = new StringBuilder();
        for (Iterator<String> i = keyIterator(); i.hasNext(); ) {
            String k = i.next();
            List vl = getValues(k);
            if ( vl != null ) {
                for ( Iterator vi = vl.iterator(); vi.hasNext(); ) {
                    sb.append(k).append(": ").append( vi.next()).append("\r\n");
                }
            }
        }
        sb.append("\r\n");

        byte[] buf = sb.toString().getBytes();

        return ByteBuffer.wrap(buf);
    }

    public int getEstimatedSize()
    {
        return estimatedSize;
    }
}
