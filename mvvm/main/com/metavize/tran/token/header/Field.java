/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.token.header;

public class Field
{
    private final String key;
    private final String value;

    // constructors -----------------------------------------------------------

    public Field(String key, String value)
    {
        this.key = key;
        this.value = value;
    }

    // accessors --------------------------------------------------------------

    public String getKey()
    {
        return key;
    }

    public String getValue()
    {
        return value;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return key + ": " + value;
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof Field)) {
            return false;
        }

        Field f = (Field)o;
        return key.equals(f.key) && value.equals(f.value);
    }

    public int hashCode()
    {
        int result = 17;
        result = 37 * result + key.hashCode();
        result = 37 * result + value.hashCode();
        return result;
    }
}
