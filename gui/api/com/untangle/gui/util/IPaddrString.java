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

package com.untangle.gui.util;

import com.untangle.uvm.node.IPaddr;

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
