/**
 * $Id$
 */

package com.untangle.uvm.app;

import java.io.Serializable;

/**
 * An immutable holder for an RFC 1049 Mime type.
 */
@SuppressWarnings("serial")
public class MimeType implements Serializable
{
    private final String mimeType;

    private String mimeTypeNoWildcard;

    /**
     * Creates a mime type from a string.
     * 
     * @param mimeType
     *        a <code>String</code> value
     */
    public MimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

    /**
     * Get the type string
     * 
     * @param mimeType
     *        The mime type
     * @return The string
     */
    public static String getType(String mimeType)
    {
        int i = mimeType.indexOf(';');
        String mt = (i < 0 ? mimeType : mimeType.substring(0, i).trim());
        if (mt != null) mt = mt.toLowerCase();
        return mt;
    }

    /**
     * Get the type
     * 
     * @return The type
     */
    public String getType()
    {
        return getType(mimeType);
    }

    /**
     * Matches * at the end of a mime-type.
     * 
     * XXX this needs some work.
     * 
     * @param val
     *        mime-type to check for a match.
     * @return boolean if mimeType is an instance of this type.
     */
    public boolean matches(String val)
    {
        if (null == val) {
            return false;
        }

        val = getType(val);
        if(val == null){
            return false;
        }

        if (isWildcard()) {
            int length = mimeTypeNoWildcard.length();

            /*
             * Not possible to wildcard match if the input string is shorter
             * than the text
             */
            if (length > val.length()) return false;

            /* The * gets stripped off at construction time */
            return val.substring(0, length).equalsIgnoreCase(mimeTypeNoWildcard);
        }

        return mimeType.equalsIgnoreCase(val);
    }

    /**
     * Check for wildcard
     * 
     * @return True if wildcard, otherwise false
     */
    public boolean isWildcard()
    {
        if (mimeTypeNoWildcard == null) {
            if (mimeType.endsWith("*")) {
                /* Remove the * at the end */
                mimeTypeNoWildcard = mimeType.substring(0, mimeType.length() - 1);
            } else {
                mimeTypeNoWildcard = mimeType;
            }
        }

        return (mimeTypeNoWildcard == mimeType) ? false : true;
    }

    /**
     * Compare to another object
     * 
     * @param o
     *        The object for comparison
     * @return True if equal, otherwise false
     */
    public boolean equals(Object o)
    {
        if (!(o instanceof MimeType)) {
            return false;
        }

        MimeType mt = (MimeType) o;
        return mimeType.equalsIgnoreCase(mt.mimeType);
    }

    /**
     * Get the has code
     * 
     * @return The hash code
     */
    public int hashCode()
    {
        return mimeType.hashCode();
    }

    /**
     * Get the string representation
     * 
     * @return The string representation
     */
    public String toString()
    {
        return mimeType;
    }
}
