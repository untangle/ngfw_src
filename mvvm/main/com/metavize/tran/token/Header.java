/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Header.java,v 1.13 2005/02/08 01:59:17 rbscott Exp $
 */

package com.metavize.tran.token;

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
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class Header implements Token
{
    private Map header = new LinkedHashMap();

    public Header() { }

    public void addField(String key, String value)
    {
        Field f = (Field)header.get(key.toUpperCase());

        if (null == f) {
            f = new Field(key);
            header.put(key.toUpperCase(), f);
        }

        f.addValue(value);
    }

    /**
     * Replace a field value.  If any values exists, they are all
     * removed and the new value is added.
     */
    public void replaceField(String key, String value)
    {
        key = key.toUpperCase();
        Field f = (Field)header.get( key );

        /* Item is not in the current header, add a new field */
        if ( null == f ) {
            f = new Field( key );
            header.put( key, f );
        } else {
            /* Remove all of the items */
            f.values.clear();
        }
        
        f.addValue( value );
    }

    public String getValue(String key)
    {
        Field f = (Field)header.get(key.toUpperCase());
        return ( null == f || f.values.size() == 0 ) ? null : (String)f.values.get(0);
    }

    public List getValues(String key)
    {
        Field f = (Field)header.get(key.toUpperCase());
        return ( null == f ) ? null : f.values;
    }

    public Iterator keyIterator()
    {
        return new Iterator()
            {
                private Iterator i = header.keySet().iterator();

                public boolean hasNext() {
                    return i.hasNext();
                }

                public Object next()
                {
                    Object k = i.next();
                    Field f = (Field)header.get(k);
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
        List values = new LinkedList();

        Field(String key)
        {
            this.key = key;
        }

        Field(String key, String value)
        {
            this.key = key;
            values.add(value);
        }

        void addValue(String key)
        {
            values.add(key);
        }
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        StringBuilder sb = new StringBuilder();
        for (Iterator i = keyIterator(); i.hasNext(); ) {
            String k = (String)i.next();
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
}
