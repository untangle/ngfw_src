/*
 * $HeadURL: svn://chef/work/src/jnetcap/test/TestTCPHook.java $
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

import com.untangle.jvector.*;
import com.untangle.jnetcap.*;
import java.util.LinkedList;

class TestTCPHook implements NetcapCallback {
    protected final int numTransforms;
    protected boolean verbose = false;

    protected static final int PORT_HEADER     = 60000;
    protected static final int PORT_HALF_WRITE = 60001;
    protected static final int TYPE_HEADER     = 1;
    protected static final int TYPE_HALF_WRITE = 2;


    public TestTCPHook( int numTransforms, boolean verbose ) {
        this.verbose = verbose;
        this.numTransforms = numTransforms;
        
        System.out.println( "Using: " + numTransforms + " transforms" );
    }

    public TestTCPHook( int numTransforms ) {
        this( numTransforms, false );
    }
    
    public void event( int sessionID )
    {
        new Thread( new TCPRunnable( sessionID )).start();
    }
    
    private class TCPRunnable implements Runnable {
        NetcapTCPSession session;

        TCPRunnable( int sessionID )
        {
            session = new NetcapTCPSession( sessionID );
        }

        public void run()
        {
        
            if ( verbose ) {
                System.out.println( "Executing TCP hook!!! with id: " + session.id());
                System.out.println( "client.client.host: " + session.clientSide().client().host().getHostAddress());
                System.out.println( "client.client.port: " + session.clientSide().client().port());
                System.out.println( "client.client.intf: " + session.clientSide().client().interfaceName());
                
                System.out.println( "client.server.host: " + session.clientSide().server().host().getHostAddress());
                System.out.println( "client.server.port: " + session.clientSide().server().port());
                System.out.println( "client.server.intf: " + session.clientSide().server().interfaceName());
            }
            
            /* Complete the connection */
            do {
                Relay currentRelay = null, prevRelay = null;
                IncomingSocketQueue prevIncomingSQ = null;
                OutgoingSocketQueue prevOutgoingSQ = null;
                int transformType;
                
                LinkedList <Relay>relayList = new LinkedList<Relay>();
                
                /* Change the server side of things a little bit */
                session.serverComplete( null, 0, null, 7000 );
                session.clientComplete();
                
                transformType = getType();
            
                if ( numTransforms == 0 ) {
                    int clientFD = session.tcpClientSide().fd();
                    int serverFD = session.tcpServerSide().fd();
                    
                    relayList.add( new Relay( new TCPSource( clientFD ), new TCPSink( serverFD )));
                    relayList.add( new Relay( new TCPSource( serverFD ), new TCPSink( clientFD )));
                } else {
                    for ( int c = 0 ; c < numTransforms ; c++ ) {
                        IncomingSocketQueue sqClientSink   = new IncomingSocketQueue();
                        IncomingSocketQueue sqServerSink   = new IncomingSocketQueue();
                        OutgoingSocketQueue sqClientSource = new OutgoingSocketQueue();
                        OutgoingSocketQueue sqServerSource = new OutgoingSocketQueue();
                        
                        if ( c == 0 ) {
                            int clientFD = session.tcpClientSide().fd();
                            relayList.add( new Relay( new TCPSource( clientFD ), sqClientSink ));
                            relayList.add( new Relay( sqClientSource, new TCPSink( clientFD )));
                        } else {
                            relayList.add( new Relay( prevOutgoingSQ, sqClientSink ));
                            relayList.add( new Relay( sqClientSource, prevIncomingSQ ));
                        }
                        
                        switch( transformType ) {
                        case TYPE_HALF_WRITE:
                            new TestHalfWriteTransform( sqClientSink, sqClientSource,
                                                        sqServerSink, sqServerSource, verbose );
                            break;
                        case TYPE_HEADER:
                            /* FALLTHROUGH */
                        default:
                            new TestTransform( sqClientSink, sqClientSource, 
                                               sqServerSink, sqServerSource, verbose );
                        }
                        
                        if ( c < ( numTransforms - 1 )) {
                            prevOutgoingSQ = sqServerSource;
                            prevIncomingSQ = sqServerSink;
                        } else {
                            int serverFD = session.tcpServerSide().fd();
                            
                            relayList.add( new Relay( sqServerSource, new TCPSink( serverFD )));
                            relayList.add( new Relay( new TCPSource( serverFD ), sqServerSink ));
                        }
                    }
                }
                
                Vector vec = new Vector( relayList );
                
                vec.timeout( 5 );
                vec.vector();
                
                /* Make sure to delete the vector */
                vec.raze();
            } while( false );
            
            /* Remove the session */
            session.raze();
        }

        public int getType()
        {
            switch ( session.clientSide().server().port()) {
            case PORT_HEADER: return TYPE_HEADER;
            case PORT_HALF_WRITE: return TYPE_HALF_WRITE;
            }
            
            return TYPE_HEADER;
        }
    }
}
