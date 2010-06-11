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

package com.untangle.uvm.node;

import java.io.Serializable;

/**
 * An immutable holder for an RFC 1049 Mime type.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
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
