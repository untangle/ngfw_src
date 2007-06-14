/*
 * $HeadURL:$
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

import com.untangle.uvm.node.IPMaddr;

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
