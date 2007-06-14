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

import java.net.InetAddress;

import com.untangle.uvm.node.IPaddr;

public class IPPortString implements Comparable<IPPortString> {

    private IPaddr ipAddr;
    private int port;

    public IPPortString(){}

    public IPPortString( IPaddr ipAddr, int port ){
        this.ipAddr = ipAddr;
        this.port = port;
    }

    public IPPortString( InetAddress inetAddress, int port ){
        this.ipAddr = new IPaddr(inetAddress);
        this.port = port;
    }

    public boolean equals(Object obj){
        if( !(obj instanceof IPPortString) )
            return false;
        else
            return 0 == compareTo( (IPPortString) obj );
    }

    public int compareTo(IPPortString ipPortString){
        if( (ipAddr == null) && (ipPortString.ipAddr == null) )
            return 0;
        else if( (ipAddr != null) && (ipPortString.ipAddr == null) )
            return 1;
        else if( (ipAddr == null) && (ipPortString.ipAddr != null) )
            return -1;
        else{
            int ipAddrComparison = ipAddr.compareTo(ipPortString.ipAddr);
            if( ipAddrComparison == -1 )
                return -1;
            else if( ipAddrComparison == 1 )
                return 1;
            else{
                if( port < ipPortString.port )
                    return -1;
                else if( port > ipPortString.port )
                    return 1;
                else
                    return 0;
            }
        }
    }

    public String toString(){
        if( ipAddr == null )
            return "";
        else
            return ipAddr.toString() + ":" + Integer.toString(port);
    }
}
