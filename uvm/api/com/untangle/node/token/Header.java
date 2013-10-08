/*
 * $HeadURL: svn://chef/work/src/uvm/api/com/untangle/node/token/Header.java $
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

    public List<String> getValues(String key)
    {
        Field f = header.get(key.toUpperCase());
        return ( null == f ) ? null : f.values;
    }

    public Iterator<String> keyIterator()
    {
        return new Iterator<String>()
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
            List<String> vl = getValues(k);
            if ( vl != null ) {
                for ( Iterator<String> vi = vl.iterator(); vi.hasNext(); ) {
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
