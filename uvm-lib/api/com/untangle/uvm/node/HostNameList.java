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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/** This list prevents duplicates. */
@SuppressWarnings("serial")
public class HostNameList implements Serializable
{
    private final LinkedList<HostName> hostNameList;

    private HostNameList( List<HostName> hostNameList )
    {
        this.hostNameList = new LinkedList<HostName>(hostNameList);
        
        /* Remove any duplicates */
        for ( Iterator<HostName> iter = this.hostNameList.iterator() ; iter.hasNext() ; ) {
            HostName hostName = iter.next();
            if ( this.hostNameList.indexOf( hostName ) != this.hostNameList.lastIndexOf( hostName )) {
                iter.remove();
            }
        }        
    }

    public HostNameList( HostNameList list )
    {
        this.hostNameList = new LinkedList<HostName>(list.hostNameList);
    }
    
    public String toString()
    { 
        String output = "";
        
        for ( Iterator<HostName> iter = hostNameList.iterator(); iter.hasNext() ; ) {
            HostName hostName = iter.next();
            
            output += hostName.toString() + ( iter.hasNext() ? " " : "" );
        }
        
        return output; 
    }

    public boolean isEmpty()
    {
        return ( hostNameList == null || hostNameList.size() == 0 );
    }

    public void add( HostName hostName ) 
    {
        if ( !hostNameList.contains( hostName ))
            hostNameList.add( hostName );
    }
    
    public void add( String hostName ) throws ParseException
    {
        add( HostName.parse( hostName ));
    }
    
    /* Merge in the elements of another list into the current list, duplicates are ignored */
    public void merge( HostNameList hl ) 
    {
        for ( Iterator<HostName> iter = hl.hostNameList.iterator() ; iter.hasNext() ; ) {
            add(iter.next());
        }
    }

    public void removeReserved()
    {
        for ( Iterator<HostName> iter = hostNameList.iterator() ; iter.hasNext() ; ) {
            HostName hostName = iter.next();
            if ( hostName.isReserved()) iter.remove();
        }
    }

    /**
     * Make a copy of all of the unqualified hostnames and append the domain to it
     *  EG: Before -> "www random.com".qualify( "mmm.net" ) => "www random.com www.mmm.net"
     */
    public void qualify( HostName domain )
    {
        List<HostName> tmp = new LinkedList<HostName>();
        for ( Iterator<HostName> iter = this.hostNameList.iterator() ; iter.hasNext() ; ) {
            HostName hostName = iter.next();
            if ( !hostName.isQualified()) {
                tmp.add( HostName.addDomain( hostName, domain ));
            }
        }

        for ( Iterator<HostName> iter = tmp.iterator() ; iter.hasNext() ; ) {
            add(iter.next());
        }
    }

    /** Retrieve the internal list of hostnames, the returned list is immutable */
    public List<HostName> getHostNameList()
    {
        return Collections.unmodifiableList( this.hostNameList );
    }
    
    public static HostNameList parse( String input ) throws ParseException
    { 
        input = input.trim();
        List<HostName> hostNameList = new LinkedList<HostName>();
        
        String[] tmp = input.split( " " );
        
        for ( int c = 0 ; c < tmp.length ; c++ ) {
            hostNameList.add( HostName.parse( tmp[c] ));
        }
        
        return new HostNameList( hostNameList );
    }
    
    public static HostNameList getEmptyHostNameList()
    {
        /* Implemented this way so you can always add domains to the empty list without modifying 
         * the global static object */
        return new HostNameList( new LinkedList<HostName>());
    }
}
