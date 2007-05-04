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

import com.untangle.mvvm.tran.IPMaddr;

public class IPMaddrString implements Comparable<IPMaddrString> {

    private IPMaddr ipMAddr; // takes precedence on comparisons
    private String unparsedString; // takes precedence on display requests

    public IPMaddrString(){}

    public IPMaddrString(String unparsedString){
        ipMAddr = IPMaddr.parse(unparsedString); // parse str for comparisons
        this.unparsedString = unparsedString;
    }

    public IPMaddrString( IPMaddr ipMAddr ){
        this.ipMAddr = ipMAddr;
    }

    public void setString(String unparsedString){
        if (null == ipMAddr) {
            ipMAddr = IPMaddr.parse(unparsedString); // parse str for comparisons
        }
        this.unparsedString = unparsedString;
    }
    public String getString(){
        return toString();
    }

    public boolean equals(Object obj){
        if( !(obj instanceof IPMaddrString) )
            return false;
        else
            return 0 == compareTo( (IPMaddrString) obj );
    }

    // cannot reliably compare on unparsedString value
    public int compareTo(IPMaddrString ipMaddrString){
        if( (ipMAddr == null) && (ipMaddrString.ipMAddr == null) )
            return 0;
        else if( (ipMAddr != null) && (ipMaddrString.ipMAddr == null) )
            return 1;
        else if( (ipMAddr == null) && (ipMaddrString.ipMAddr != null) )
            return -1;
        else
            return ipMAddr.compareTo(ipMaddrString.ipMAddr);
    }

    public String toString(){
        if( unparsedString != null )
            return unparsedString;
        else if( ipMAddr == null )
            return "";
        else
            return ipMAddr.toString();
    }
}
