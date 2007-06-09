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

package com.untangle.mvvm.tran;

import java.io.Serializable;


        
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

/** This list prevents duplicates. */
public class HostNameList implements Serializable
{
    private final List hostNameList;

    private HostNameList( List hostNameList )
    {
        this.hostNameList = hostNameList;
        
        /* Remove any duplicates */
        for ( Iterator iter = this.hostNameList.iterator() ; iter.hasNext() ; ) {
            HostName hostName = (HostName)iter.next();
            if ( this.hostNameList.indexOf( hostName ) != this.hostNameList.lastIndexOf( hostName )) {
                iter.remove();
            }
        }        
    }

    public HostNameList( HostNameList list )
    {
        this.hostNameList = new LinkedList( list.hostNameList );
    }
    
    public String toString()
    { 
        String output = "";
        
        for ( Iterator iter = hostNameList.iterator(); iter.hasNext() ; ) {
            HostName hostName = (HostName)iter.next();
            
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
        for ( Iterator iter = hl.hostNameList.iterator() ; iter.hasNext() ; ) {
            add((HostName)iter.next());
        }
    }

    public void removeReserved()
    {
        for ( Iterator iter = hostNameList.iterator() ; iter.hasNext() ; ) {
            HostName hostName = (HostName)iter.next();
            if ( hostName.isReserved()) iter.remove();
        }
    }

    /**
     * Make a copy of all of the unqualified hostnames and append the domain to it
     *  EG: Before -> "www random.com".qualify( "mmm.net" ) => "www random.com www.mmm.net"
     */
    public void qualify( HostName domain )
    {
        List tmp = new LinkedList();
        for ( Iterator iter = this.hostNameList.iterator() ; iter.hasNext() ; ) {
            HostName hostName = (HostName)iter.next();
            if ( !hostName.isQualified()) {
                tmp.add( HostName.addDomain( hostName, domain ));
            }
        }

        for ( Iterator iter = tmp.iterator() ; iter.hasNext() ; ) {
            add((HostName)iter.next());
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
        List hostNameList = new LinkedList();
        
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
        return new HostNameList( new LinkedList());
    }
}
