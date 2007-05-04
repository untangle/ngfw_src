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

package com.untangle.tran.mime;

/**
 * Wrapper around a String which is already lower-cased.  The contained
 * String cannot be null.  Useful for comparisons where one is constantly
 * calling "toLowerCase" or "equalsIgnoreCase".
 */
public class LCString {

    /**
     * String wrapped by this instance.  This String cannot
     * be null.
     */
    public final String str;

    public LCString(String mayBeMixedCase)
        throws NullPointerException {
        this.str = mayBeMixedCase.toLowerCase();
    }

    @Override
    public String toString() {
        return str;
    }

    @Override
    public int hashCode() {
        return str.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(obj instanceof LCString) {
            LCString other = (LCString) obj;
            return str.equals(((LCString) obj).str);
        }
        if(obj instanceof String) {
            return str.equals((String) obj);
        }
        return false;
    }
}
