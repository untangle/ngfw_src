/*
 * $HeadURL: svn://chef/work/src/jnetcap/test/TestHalfWriteTransform.java $
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
    
public class TestHalfWriteTransform implements SocketQueueListener
{
    private String name   = "Half write, 1/4 write buffer, 1/4 read buffer";
    private final String DEBUG_PREFIX = "TRAN(" + this + "): ";
    
    private boolean verbose = false;
    private final LinkedList serverCrumbs = new LinkedList();
    private final LinkedList clientCrumbs = new LinkedList();

    private final IncomingSocketQueue clientIncomingSocketQueue;
    private final OutgoingSocketQueue clientOutgoingSocketQueue;
    private final IncomingSocketQueue serverIncomingSocketQueue;
    private final OutgoingSocketQueue serverOutgoingSocketQueue;
    
    public TestHalfWriteTransform ( IncomingSocketQueue c2s_r, OutgoingSocketQueue c2s_w, 
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

    public TestHalfWriteTransform ( IncomingSocketQueue c2s_r, OutgoingSocketQueue c2s_w, 
                                    IncomingSocketQueue s2c_r, OutgoingSocketQueue s2c_w )
    {
        this( c2s_r, c2s_w, s2c_r, s2c_w, false );
    }
    

    public void event( IncomingSocketQueue in )
    {
        OutgoingSocketQueue out = (OutgoingSocketQueue)in.sq().attachment();

        /* True if the incoming socket queue should be re-enabled */
        boolean enable = true;
        
        Crumb obj = in.peek();
        
        if (this.verbose)
            System.out.println( DEBUG_PREFIX + "Transform \"" + name + "\" processing: " + obj);

        if ( obj instanceof DataCrumb ) {
            DataCrumb origCrumb = (DataCrumb)obj;

            if ( this.verbose ) {
                System.out.println( DEBUG_PREFIX + "Received a dataCrumb of offset: " + origCrumb.offset() + 
                                    " limit: " + origCrumb.limit());
            }

            if (( origCrumb.offset() == 0 ) && ( origCrumb.limit() > 3 )) {
                /* Non-Buffered incoming crumb that is large enough to cut into thirds */           
                DataCrumb writeCrumb;
                DataCrumb outCrumb;
                byte[] data;
                int limit;
                data = origCrumb.data();
                limit = origCrumb.limit();
                
                /* Indicate to not re-enable the incoming socket queue */
                enable = false;
                
                /* Send the first half of the current data crumb */
                outCrumb = new DataCrumb( data, 0, limit >> 1 );
                
                /* Divide the second half in half for the write crumb and the original */
                writeCrumb = new DataCrumb( data, limit >> 1, ( limit >> 1 ) + ( limit >> 2 ));
                
                /* Advance the original crumb, and cache it */
                origCrumb.offset(( limit >> 1 ) + ( limit >> 2 ));
                                
                /* Buffer half of the data, and write the other half */
                if ( in == clientIncomingSocketQueue ) {
                    if ( verbose ) System.out.println( DEBUG_PREFIX + "Buffering client crumb" );
                    clientCrumbs.addLast( writeCrumb );
                } else if ( in == serverIncomingSocketQueue ) {
                    if ( verbose ) System.out.println( DEBUG_PREFIX + "Buffering server crumb" );
                    serverCrumbs.addLast( writeCrumb );
                } else {
                    throw new IllegalStateException( DEBUG_PREFIX + "Unknown OutgoingSocketQueue: " + out );
                }

                /* Write out the crumb destined for output */
                obj = outCrumb;
            }
        }
        
        /* Take the crumb out of the socket queue */
        if ( enable ) {
            in.read();
        } else {
            in.disable();
        }

        out.write( obj );
    }
    
    public void event( OutgoingSocketQueue out )
    {
        LinkedList crumbs;
        IncomingSocketQueue in;

        if ( verbose ) {
            System.out.println( DEBUG_PREFIX + 
                                "Transform \"" + name + "\" Received event on Outgoing Socket Queue" );
        }

        if ( out == clientOutgoingSocketQueue ) {
            if ( verbose ) System.out.println( DEBUG_PREFIX + 
                                               "OutgoingSocketQueue event on client, reading from server" );
            crumbs = serverCrumbs;
            in = serverIncomingSocketQueue;
        } else if ( out == serverOutgoingSocketQueue ) {
            if ( verbose ) System.out.println( DEBUG_PREFIX + 
                                               "OutgoingSocketQueue event on server, reading from client" );
            crumbs = clientCrumbs;
            in = clientIncomingSocketQueue;
        } else {
            throw new IllegalStateException( DEBUG_PREFIX + "Unknown OutgoingSocketQueue: " + out );
        }
        
        /* No more crumbs to write */
        if ( crumbs.isEmpty()) {
            if ( verbose ) System.out.println( DEBUG_PREFIX + "No more crumbs to write" );

            /* Re-enable read events on the incoming socket queue */
            in.enable();
        
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
}
