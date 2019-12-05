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
    private Map<String, Field> header = new LinkedHashMap<>();

    /**
     * An individual field of a HTTP header
     */
    private static class Field
    {
        String key;

        private List<String> values = new LinkedList<>();

        /**
         * create a Field
         * @param key - the key
         */
        Field(String key)
        {
            this.key = key;
        }

        /**
         * Add a value to a field
         * @param value - the value to add
         */
        void addValue(String value)
        {
            values.add(value);
        }
    }
    
    /**
     * Create an empty HeaderToken
     */
    public HeaderToken() { }

    /**
     * Add a field to the header
     * This is used during construction of the HeaderToken
     * @param key - the key
     * @param value - the value for the key
     */
    public void addField(String key, String value)
    {
        Field f = header.get(key.toUpperCase());

        if (null == f) {
            f = new Field(key);
            header.put(key.toUpperCase(), f);
        } 

        f.addValue(value);
    }

    /**
     * Remove a field from the header
     * Has no effect if the key is not found
     * @param key - the key
     */
    public void removeField(String key)
    {
        Field f = header.remove(key.toUpperCase());
    }

    /**
     * Replace a field value.  If any values exists, they are all
     * removed and the new value is added.
     * @param key - the key
     * @param value - the value for the key
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

    /**
     * Get the value for the specified key or null if not found
     * Returns the first value if a key has multiple values
     * @param key - the key
     * @return the value
     */
    public String getValue(String key)
    {
        Field f = header.get(key.toUpperCase());
        return (null == f || f.values.size() == 0) ? null : f.values.get(0);
    }

    /**
     * Get the values for the specified key or null if not found
     * @param key - the key
     * @return the values
     */
    public List<String> getValues(String key)
    {
        Field f = header.get(key.toUpperCase());
        return ( null == f ) ? null : f.values;
    }

    /**
     * Get the values for the specified key or null if not found
     * @param key - the key
     * @param values - list of values to set
     * @return nothing
     */
    public void setValues(String key, List<String> values)
    {
        Field f = new Field(key.toUpperCase());
        for ( String value: values) {
            f.addValue(value);
        }
        header.put(key.toUpperCase(), f);
    }

    /**
     * Get a iterator for the keySet
     * @return an iterator
     */
    public Iterator<String> keyIterator()
    {
        return new Iterator<String>()
            {
                private Iterator<String> i = header.keySet().iterator();

                /**
                 * True if has a next, false otherwise
                 * @return boolean
                 */
                public boolean hasNext()
                {
                    return i.hasNext();
                }

                /**
                 * get the next key
                 * @return next key
                 */
                public String next()
                {
                    Object k = i.next();
                    Field f = header.get(k);
                    return f.key;
                }

                /**
                 * remove this key
                 */
                public void remove()
                {
                    i.remove();
                }
            };
    }

    /**
     * Get the ByteBuffer equivalent of the HeaderToken
     * @return the ByteBuffer
     */
    public String getString()
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
        return sb.toString();
    }

    /**
     * Get the ByteBuffer equivalent of the HeaderToken
     * @return the ByteBuffer
     */
    public ByteBuffer getBytes()
    {
        byte[] buf = getString().getBytes();

        return ByteBuffer.wrap(buf);
    }

}
