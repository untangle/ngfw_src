/**
 * $Id: MimeType.java 35447 2013-07-29 17:24:43Z dmorris $
 */
package com.untangle.uvm.node;

import java.io.Serializable;

/**
 * An immutable holder for an RFC 1049 Mime type.
 *
 */
@SuppressWarnings("serial")
public class MimeType implements Serializable
{

    private final String mimeType;

    private String mimeTypeNoWildcard;

    /**
     * Creates a mime type from a string.
     *
     * @param mimeType a <code>String</code> value
     */
    public MimeType(String mimeType)
    {
        // XXX should validate & parse into components.
        this.mimeType = mimeType;
    }

    // static methods ---------------------------------------------------------

    public static String getType(String mimeType)
    {
        int i = mimeType.indexOf(';');
        return 0 > i ? mimeType : mimeType.substring(0, i).trim();
    }

    // Business methods -------------------------------------------------------

    public String getType()
    {
        return getType(mimeType);
    }

    /**
     * Matches * at the end of a mime-type.
     *
     * XXX this needs some work.
     *
     * @param val mime-type to check for a match.
     * @return boolean if mimeType is an instance of this type.
     */
    public boolean matches(String val)
    {
        if (null == val) {
            return false;
        }

        val = getType(val);

        if (isWildcard()) {
            int length = mimeTypeNoWildcard.length();

            /* Not possible to wildcard match if the input string is
             * shorter than the text */
            if (length > val.length())
                return false;

            /* The * gets stripped off at construction time */
            return val.substring(0, length).equalsIgnoreCase(mimeTypeNoWildcard);
        }

        return mimeType.equalsIgnoreCase(val);
    }

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

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof MimeType)) {
            return false;
        }

        MimeType mt = (MimeType)o;
        return mimeType.equalsIgnoreCase(mt.mimeType);
    }

    public int hashCode()
    {
        return mimeType.hashCode();
    }

    public String toString()
    {
        return mimeType;
    }
}
