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

package com.untangle.gui.util;


public class LongString implements Comparable<LongString> {

    private Long value;
    private String valueString;

    public LongString(Long value, String valueString){
        this.value = value;
        this.valueString = valueString;
    }

    public String toString(){ return valueString; }

    public boolean equals(Object obj){
        if( !(obj instanceof LongString) )
            return false;
        else
            return 0 == compareTo( (LongString) obj );
    }

    public int compareTo(LongString longString){
        return value.compareTo(longString.value);
    }

}
