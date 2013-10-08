/*
 * $HeadURL: svn://chef/work/src/jnetcap/test/TestTransform.java $
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */


import java.util.LinkedList;

import com.untangle.jvector.*;
import com.untangle.jnetcap.*;
    
public class TestTransform implements SocketQueueListener
{
    private String name   = "Plus 1 Data";
    private static final String header = "---Header--- ";
    private static final byte[] headerData = header.getBytes();
    private final DataCrumb headerCrumb = new DataCrumb( header + this + "\n" );

    private final String DEBUG_PREFIX = "TRAN(" + this + "): ";
    
    private boolean verbose = false;
    private final LinkedList serverCrumbs = new LinkedList();
    private final LinkedList clientCrumbs = new LinkedList();

    private final IncomingSocketQueue clientIncomingSocketQueue;
    private final OutgoingSocketQueue clientOutgoingSocketQueue;
    private final IncomingSocketQueue serverIncomingSocketQueue;
    private final OutgoingSocketQueue serverOutgoingSocketQueue;
    
    public TestTransform ( IncomingSocketQueue c2s_r, OutgoingSocketQueue c2s_w, 
                           IncomingSocketQueue s2c_r, OutgoingSocketQueue s2c_w, 
                           boolean verbose )
    {
        c2s_r.sq().registerListener(this);
        c2s_w.sq().registerListener(this);
        c2s_r.sq().attach(s2c_w);

        s2c_r.sq().registerListener(this);
        s2c_w.sq().registerListener(this);
        s2c_r.sq().attach(c2s_w);

        clientIncomingSocketQueue = c2s_r;
        clientOutgoingSocketQueue = c2s_w;

        serverIncomingSocketQueue = s2c_r;
        serverOutgoingSocketQueue = s2c_w;

        this.verbose = verbose;
    }

    public TestTransform ( IncomingSocketQueue c2s_r, OutgoingSocketQueue c2s_w, 
                           IncomingSocketQueue s2c_r, OutgoingSocketQueue s2c_w )
    {
        this( c2s_r, c2s_w, s2c_r, s2c_w, false );
    }
    

    public void event( IncomingSocketQueue in )
    {
        OutgoingSocketQueue out = (OutgoingSocketQueue)in.sq().attachment();
        
        Crumb obj = in.read();
        
        if (this.verbose)
            System.out.println( DEBUG_PREFIX + "Transform \"" + name + "\" passing: " + obj);

        if ( obj instanceof DataCrumb ) {
            /* XOR The data */
            byte[] data;
            DataCrumb dataCrumb = (DataCrumb)obj;

            data = dataCrumb.data();
            
            if ( this.verbose ) {
                System.out.println( DEBUG_PREFIX + "Received a dataCrumb of size: " + dataCrumb.limit());
            }

            /* If this is not the header, buffer the data and write a header on the other side */
            if ( !isHeader( data )) {
                /* Add one to everything but the last piece (\n) of the message */
                for ( int c = dataCrumb.limit() - 1 ; c-- > 0 ; ) {
                    int t = data[c];
                    t = t +1;
                    data[c] = (byte)t;
                }
            
                if ( in == clientIncomingSocketQueue ) {
                    if ( verbose ) System.out.println( DEBUG_PREFIX + "Buffering client crumb" );
                    clientCrumbs.addLast( obj );
                } else if ( in == serverIncomingSocketQueue ) {
                    if ( verbose ) System.out.println( DEBUG_PREFIX + "Buffering server crumb" );
                    serverCrumbs.addLast( obj );
                } else {
                    throw new IllegalStateException( DEBUG_PREFIX + "Unknown OutgoingSocketQueue: " + out );
                }
                
                obj = headerCrumb;
            }
        }
        
        out.write( obj );
    }
    
    public void event( OutgoingSocketQueue out )
    {
        LinkedList crumbs;

        if ( verbose ) {
            System.out.println( DEBUG_PREFIX + 
                                "Transform \"" + name + "\" Received event on Outgoing Socket Queue" );
        }

        if ( out == clientOutgoingSocketQueue ) {
            if ( verbose ) System.out.println( DEBUG_PREFIX + 
                                               "OutgoingSocketQueue event on client, reading from server" );
            crumbs = serverCrumbs;
        } else if ( out == serverOutgoingSocketQueue ) {
            if ( verbose ) System.out.println( DEBUG_PREFIX + 
                                               "OutgoingSocketQueue event on server, reading from client" );
            crumbs = clientCrumbs;
        } else {
            throw new IllegalStateException( DEBUG_PREFIX + "Unknown OutgoingSocketQueue: " + out );
        }
        
        /* No more crumbs to write */
        if ( crumbs.isEmpty()) {
            if ( verbose ) System.out.println( DEBUG_PREFIX + "No more crumbs to write" );
            return;
        }

        Crumb crumb = (Crumb)crumbs.removeFirst();

        /* Write out the buffered crumb */
        out.write( crumb );
    }
    
    public void event( IncomingSocketQueue in, OutgoingSocketQueue out )
    {
        /* Not much to do when data is coming back */
        if ( this.verbose ) {
            System.out.println( DEBUG_PREFIX + "Transform \"" + name + "\" Received dual SocketQueue event" );
        }
    }

    public void shutdownEvent( IncomingSocketQueue in )
    {
        if ( this.verbose ) {
            System.out.println( DEBUG_PREFIX + "Transform \"" + name + 
                                "\" Received IncomingSocketQueue shutdown event: " + in );
        }
    }

    public void shutdownEvent( OutgoingSocketQueue out )
    {
        if ( this.verbose ) {
            System.out.println( DEBUG_PREFIX + "Transform \"" + name + 
                                "\" Received OutgoingSocketQueue shutdown event: " + out );
        }        
    }


    public void name (String s)
    {
        this.name = s;
    }

    public void verbose (boolean b)
    {
        this.verbose = b;
    }

    private boolean isHeader( byte[] data )
    {
        for ( int c = 0 ; c < headerData.length ; c++ ) {
            if ( headerData[c] != data[c] ) return false;
        }
            
        return true;
    }

}
