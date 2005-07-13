/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;

// To verify that there are no overlapping IPs in the final table run this query.
// select
//     t1.address,t2.address,
//     t1.start_time,t2.start_time,
//     t1.end_time,t2.end_time,
//     t1.name,t2.name 
// from 
//     dhcp_address_map as t1, dhcp_address_map as t2 
// where 
// t1.address = t2.address and t1.id != t2.id and 
//     (( t1.start_time = t2.start_time and t1.end_time = t2.start_time ) or 
//      ( t1.start_time = t2.end_time and t1.end_time = t2.end_time));
//
// To determine which hostname maps to an address at a specific time run
// select name from dhcp_address_map where address=? and start_time <= ? and end_time >= ?

public class DhcpMap
{
    private static final Logger logger = Logger.getLogger( DhcpMap.class );
    private static final DhcpMap INSTANCE = new DhcpMap();
    
    private static final String TABLE_NAME = "dhcp_address_map";

    static final int COL_TIMESTAMP    = 1;
    static final int COL_END_OF_LEASE = 2;
    static final int COL_INET_ADDRESS = 3;
    static final int COL_HOSTNAME     = 4;
    static final int COL_EVENT_TYPE   = 5;
    
    static final int EVT_TYPE_REGISTER = 0;
    static final int EVT_TYPE_RENEW    = 1;
    static final int EVT_TYPE_EXPIRE   = 2;
    static final int EVT_TYPE_RELEASE  = 3;

    private static final String ABSOLUTE_QUERY = 
        "SELECT evt.time_stamp, lease.end_of_lease, lease.ip, lease.hostname, " +
        " CASE WHEN (lease.event_type = 0) THEN 0 ELSE 3 END AS event_type " +
        " FROM tr_nat_evt_dhcp_abs_leases AS glue, tr_nat_evt_dhcp_abs AS evt, dhcp_abs_lease AS lease " +
        " WHERE glue.event_id=evt.event_id AND glue.lease_id = lease.event_id " + 
        "  AND (( ? <= evt.time_stamp and evt.time_stamp <= ? ) or " +
        " (( ? <= lease.end_of_lease and lease.end_of_lease <= ? ))) order by evt.time_stamp";

    private static final String RELATIVE_QUERY =
        "SELECT evt.time_stamp, evt.end_of_lease, evt.ip, evt.hostname, evt.event_type " +
        "FROM tr_nat_evt_dhcp AS evt WHERE ( ? <= evt.time_stamp and evt.time_stamp <= ? ) OR " +
        " ( ? <= evt.end_of_lease AND evt.end_of_lease <= ? ) order by evt.time_stamp";

    private static final String CREATE_TEMPORARY_TABLE = 
        "CREATE TABLE " + TABLE_NAME + "( " +
        " id         SERIAL8 NOT NULL," +
        " address    INET NOT NULL," +
        " name       VARCHAR(255)," +
        " start_time TIMESTAMP NOT NULL," +
        " end_time   TIMESTAMP," +
        " PRIMARY KEY (id))";

    private static final String DROP_TEMPORARY_TABLE = 
        "DROP TABLE " + TABLE_NAME;

    private static final String INSERT_LEASE = 
        "INSERT INTO " + TABLE_NAME + " ( address, name, start_time, end_time ) "+ 
        "values ( ?, ?, ?, ? )";
    
    private DhcpMap()
    {
    }

    public static void main( String[] args )
    {        
        try {
            Connection conn = null;
            
            BasicConfigurator.configure();
            
            Class.forName( "org.postgresql.Driver" );
            conn = DriverManager.getConnection( "jdbc:postgresql://localhost/mvvm", "metavize", "foo" );
            
            /* Regenerate address map */
            INSTANCE.generateAddressMap( conn, Timestamp.valueOf( args[0] ), Timestamp.valueOf( args[1] ));
        } catch ( ClassNotFoundException e ) {
            logger.warn( "Could not load the Postgres JDBC driver" );
            System.exit( 1 );
        } catch ( SQLException e ) {
            logger.warn( "Could not get JDBC connection", e );
            System.exit( 1 );
        } catch ( IllegalArgumentException e ) {
            logger.warn( "Unable to parse the timestamp", e );
            System.exit( 1 );
        }
    }

    public void generateAddressMap( Connection conn, Timestamp start, Timestamp end )
    {
        /* Always try to delete the table first */
        deleteAddressMap( conn );

        try {
            /* Create a new mapping between an address and a list of address maps */
            Map<InetAddress,List<Lease>> map = new HashMap<InetAddress,List<Lease>>();

            Statement stmt = conn.createStatement();
            stmt.executeUpdate( CREATE_TEMPORARY_TABLE );
            generateAbsoluteLeases( conn, start, end, map );
            generateRelativeLeases( conn, start, end, map );
            writeLeases( conn, map );
        } catch ( SQLException e ) {
            logger.warn( "Unable to generate address map", e );
        }
    }
    
    public void deleteAddressMap( Connection conn )
    {
        logger.debug( "Deleting address map." );

        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate( DROP_TEMPORARY_TABLE );
        } catch ( SQLException e ) {
            logger.warn( "Unable to delete address map", e );
        }        
    }
    
    private void generateAbsoluteLeases( Connection conn, Timestamp start, Timestamp end, 
                                         Map<InetAddress,List<Lease>> map ) 
        throws SQLException
    {
        logger.debug( "Generating asbolute leases." );

        generateLeases( ABSOLUTE_QUERY, conn, start, end, map );
    }
    
    private void generateRelativeLeases( Connection conn, Timestamp start, Timestamp end,
                                         Map<InetAddress,List<Lease>> map )
        throws SQLException
    {
        logger.debug( "Generating relative leases." );

        generateLeases( RELATIVE_QUERY, conn, start, end, map );
    }

    private void generateLeases( String query, Connection conn, Timestamp start, Timestamp end,
                                 Map<InetAddress,List<Lease>> map )
        throws SQLException
    {
        ResultSet rs = null;

        try {
            rs = executeQuery( conn, query, start, end );

            LeaseEvent event = new LeaseEvent();
            
            while ( rs.next()) {
                try {
                    event.nextLeaseEvent( rs );
                    insertLease( map, event );
                } catch ( UnknownHostException e ) {
                    logger.warn( "Error parsing an ip address", e );
                } catch ( ParseException e ) {
                    logger.warn( "A lease was invalid", e );
                }
            }
            
        } finally {
            if ( rs == null ) rs.close();
        }        
    }


    private void writeLeases( Connection conn, Map<InetAddress,List<Lease>> map ) 
        throws SQLException
    {
        PreparedStatement stmt = conn.prepareStatement( INSERT_LEASE );

        for ( Iterator<Map.Entry<InetAddress,List<Lease>>> iter = map.entrySet().iterator() ; 
              iter.hasNext() ; ) {
            Map.Entry<InetAddress,List<Lease>> entry = iter.next();
            writeLease( stmt, entry.getKey(), entry.getValue());
        }
    }

    private void writeLease( PreparedStatement stmt, InetAddress ip, List<Lease> leaseList ) 
        throws SQLException
    {
        for ( Iterator<Lease> iter = leaseList.iterator() ; iter.hasNext() ; ) {
            Lease lease = iter.next();
            lease.fillStatement( ip, stmt );

            // logger.debug( "IP: " + ip + " (" + lease.hostname + ":" + lease.start + " -> "+ 
            //               lease.eol + ")" );

            stmt.executeUpdate();
        }
    }

    private void insertLease( Map<InetAddress, List<Lease>> map, LeaseEvent event )
    {
        switch ( event.eventType ) {
        case EVT_TYPE_REGISTER:
        case EVT_TYPE_RENEW:
            mergeEvent( map, event );
            break;
            
        case EVT_TYPE_RELEASE:
        case EVT_TYPE_EXPIRE:
            truncateEvent( map, event );
            break;

        default:
            logger.error( "Unknown event type: " + event.eventType );
        }
    }

    private void mergeEvent( Map<InetAddress, List<Lease>> map, LeaseEvent event )
    {
        /* Retrieve the list for this ip */
        List<Lease> leaseList = map.get( event.ip );
        
        if ( leaseList == null ) {
            leaseList = new LinkedList<Lease>();

            /* Add the lease list to the map */            
            map.put( event.ip, leaseList );
            
            /* Add the lease to the list */
            leaseList.add( new Lease( event ));
            return;
        }
        
        for ( ListIterator<Lease> iter = leaseList.listIterator() ; iter.hasNext() ;  ) {
            Lease lease = iter.next();
            
            boolean isSameHostname = ( event.hostname.equals( lease.hostname ));

            /* If lease is in the range, then truncate it */
            /* Case 1. Lease is completely before the next lease */
            if ( lease.after( event )) {
                /* Move back one */
                iter.previous();

                /* Insert the item before the current one */
                iter.add( new Lease( event ));
                return;
            } /* Case 2. Lease started before, but ends after the next lease */ 
            else if ( lease.intersectsBefore( event )) {
                if ( isSameHostname ) {
                    /* Move the lease back */
                    lease.start = event.start;
                } else {
                    iter.previous();
                    /* Set the end of the event to the start of the next lease */
                    event.eol = lease.start;
                    iter.add( new Lease( event ));
                }
                return;
            } /* Case 3. Lease starts and stops inside this lease */
            else if ( lease.encompass( event )) {
                if ( isSameHostname ) {
                    return;
                } else {
                    /* The old lease ends at the start of the new lease */
                    lease.eol = event.start;
                    iter.add( new Lease( event ));
                }
                return;
            } /* Case 4. Lease starts inside lease, and ends after lease */
            else if ( lease.intersectsAfter( event )) {
                if ( isSameHostname ) {
                    /* Advance the lease forward */
                    lease.eol = event.eol;
                } else {
                    /* move the end back */
                    lease.eol = event.start;
                    lease = new Lease( event );
                    iter.add( lease );
                }

                if ( iter.hasNext()) {
                    /* Try to merge */
                    Lease nextLease = iter.next();
                    
                    if ( nextLease.start.after( lease.start ) && nextLease.start.before( lease.eol )) {
                        if ( nextLease.hostname.equals( lease.hostname )) {
                            /* Remove the next item */
                            iter.remove();
                            lease.eol = nextLease.eol;
                        } else {
                            /* Move the end of lease back */
                            lease.eol = nextLease.start;
                        }
                    }
                }
                return;
            } /* Case 5. Lease starts before and ends after lease */
            else if ( lease.encompassed( event )) {
                /* Move the start back */
                lease.start = event.start;
                return;
            }
        }
        
        /* Add the lease to the list */
        leaseList.add( new Lease( event ));
    }

    private void truncateEvent( Map<InetAddress, List<Lease>> map, LeaseEvent event )
    {
        /* Retrieve the list for this ip */
        List<Lease> leaseList = map.get( event.ip );

        /* Nothing to do */
        if ( leaseList == null ) return;
        
        for ( Iterator<Lease> iter = leaseList.iterator() ; iter.hasNext() ;  ) {
            Lease lease = iter.next();

            /* If lease is in the range, then truncate it */
            if ( lease.start.before( event.start ) && lease.eol.after( event.start )) {
                lease.eol = event.start;
                break;
            }
        }
    }

    private ResultSet executeQuery( Connection conn, String query, Timestamp start, Timestamp end )
        throws SQLException
    {
        PreparedStatement stmt = conn.prepareStatement( query );
        stmt.setTimestamp( 1, start );
        stmt.setTimestamp( 2, end );
        stmt.setTimestamp( 3, start );
        stmt.setTimestamp( 4, end );
        return stmt.executeQuery();
    }
}

class LeaseEvent
{
    InetAddress ip  = null;
    Timestamp start = null;
    Timestamp eol   = null;
    String hostname = null;
    int eventType   = -1;

    LeaseEvent()
    {
    }

    /* Convenient way to reuse a lease event */
    void nextLeaseEvent( ResultSet rs )
        throws UnknownHostException, SQLException, ParseException
    {
        this.ip        = InetAddress.getByName( rs.getString( DhcpMap.COL_INET_ADDRESS ));
        this.start     = rs.getTimestamp( DhcpMap.COL_TIMESTAMP );
        this.eol       = rs.getTimestamp( DhcpMap.COL_END_OF_LEASE );
        this.hostname  = rs.getString( DhcpMap.COL_HOSTNAME );
        this.eventType = rs.getInt( DhcpMap.COL_EVENT_TYPE );
        
        if (( this.eventType == DhcpMap.EVT_TYPE_REGISTER || 
              this.eventType == DhcpMap.EVT_TYPE_RENEW ) &&
            this.start.after( this.eol )) throw new ParseException( "Lease starts after it ends" );
    }

    static LeaseEvent makeLeaseEvent( ResultSet rs )
        throws UnknownHostException, SQLException, ParseException
    {
        LeaseEvent event = new LeaseEvent();
        event.nextLeaseEvent( rs );
        return event;
    }

}

class Lease
{
    Timestamp start;
    Timestamp eol;
    final String    hostname;
    
    Lease( Timestamp start, Timestamp eol, String hostname )
    {
        this.start    = start;
        this.eol      = eol;
        this.hostname = hostname;
    }

    Lease( LeaseEvent event )
    {
        this.start    = event.start;
        this.eol      = event.eol;
        this.hostname = event.hostname;
        
    }
    
    boolean after( LeaseEvent event )
    {
        return start.after( event.eol );
    }

    boolean intersectsBefore( LeaseEvent event )
    {
        return ( this.start.after( event.start ) && this.start.before( event.eol ) &&
                 ( this.eol.after( event.eol ) || this.eol.equals( event.eol )));
    }

    boolean encompass( LeaseEvent event )
    {
        return (( this.start.equals( event.start ) || this.start.before( event.start )) &&
                ( this.eol.equals( event.eol ) || this.eol.after( event.eol )));
    }

    boolean intersectsAfter( LeaseEvent event )
    {
        return (( this.start.before( event.start ) && this.eol.after( event.start )) &&
                ( this.eol.equals( event.eol ) || this.eol.before( event.eol )));
    }

    boolean encompassed( LeaseEvent event )
    {
        return (( this.start.equals( event.start ) || this.start.after( event.start )) &&
                ( this.eol.equals( event.eol ) || this.eol.before( event.eol )));
    }

    void fillStatement( InetAddress ip, PreparedStatement stmt )
        throws SQLException
    {
        stmt.setString( 1, ip.getHostAddress());
        stmt.setString( 2, this.hostname );
        stmt.setTimestamp( 3, start );
        stmt.setTimestamp( 4, eol );
    }
}

class ParseException extends Exception
{
    ParseException( String msg )
    {
        super( msg );
    }
}
