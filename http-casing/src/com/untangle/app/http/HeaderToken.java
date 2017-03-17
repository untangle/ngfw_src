/**
 * $Id$
 */
package com.untangle.app.http;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.vnet.Token;

/**
 * Holds an RFC 822 header, as used by HTTP.
 */
public class HeaderToken implements Token
{
    private Map<String, Field> header = new LinkedHashMap<String, Field>();

    public HeaderToken() { }

    public void addField(String key, String value)
    {
        Field f = header.get(key.toUpperCase());

        if (null == f) {
            f = new Field(key);
            header.put(key.toUpperCase(), f);
        } 

        f.addValue(value);
    }

    public void removeField(String key)
    {
        Field f = header.remove(key.toUpperCase());
    }

    /**
     * Replace a field value.  If any values exists, they are all
     * removed and the new value is added.
     */
    public void replaceField(String key, String value)
    {
        Field f = header.get(key.toUpperCase());

        /* Item is not in the current header, add a new field */
        if (null == f) {
            f = new Field(key);
            header.put(key.toUpperCase(), f);
        } else {
            /* Remove all of the items */
            f.values.clear();
        }

        f.addValue( value );
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

        Field(String key)
        {
            this.key = key;
        }

        void addValue(String value)
        {
            values.add(value);
        }
    }

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
}
