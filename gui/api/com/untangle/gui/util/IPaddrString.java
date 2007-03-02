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

import com.untangle.mvvm.tran.IPaddr;

public class IPaddrString implements Comparable<IPaddrString> {

    private IPaddr ipAddr;
    private String unparsedString;
    private String emptyString;

    public IPaddrString(){}

    public IPaddrString(String unparsedString){
	this.unparsedString = unparsedString;
    }

    public IPaddrString( IPaddr ipAddr ){
	this.ipAddr = ipAddr;
    }

    public void setString(String unparsedString){
	this.unparsedString = unparsedString;
    }
    public String getString(){
	return toString();
    }

    public void setEmptyString(String s){
        emptyString = s;
    }

    public boolean equals(Object obj){
	if( !(obj instanceof IPaddrString) )
	    return false;
	else
	    return 0 == compareTo( (IPaddrString) obj );
    }

    public int compareTo(IPaddrString ipAddrString){
	if( (unparsedString != null) && (ipAddrString.unparsedString != null) )
	    return unparsedString.compareTo(ipAddrString.unparsedString);
	else if( (unparsedString == null) && (ipAddrString.unparsedString != null) )
	    return 1;
	else if( (unparsedString != null) && (ipAddrString.unparsedString == null) )
	    return -1;
        else if( (ipAddr == null) && (ipAddrString.ipAddr == null) )
	    return 0;
	else if( (ipAddr != null) && (ipAddrString.ipAddr == null) )
	    return 1;
	else if( (ipAddr == null) && (ipAddrString.ipAddr != null) )
	    return -1;
	else
	    return ipAddr.compareTo(ipAddrString.ipAddr);
    }

    public String toString(){
        if( unparsedString != null )
            return unparsedString;
        else if( ipAddr == null ){
            if(emptyString == null)
                return "";
            else
                return emptyString;
        }
        else
            return ipAddr.toString();
    }
}
