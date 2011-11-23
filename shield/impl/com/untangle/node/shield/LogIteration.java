/*
 * $Id$
 */
package com.untangle.node.shield;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Date;
import java.util.List;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.ParseException;

import com.untangle.uvm.ArgonException;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.networking.InterfaceConfiguration;

public class LogIteration
{
    private final JSONObject start;
    private final Date startTime;
    private final Date endTime;

    private final ShieldStatisticEvent statisticEvent;
    private final List<ShieldRejectionEvent> rejectionEvents;

    private LogIteration( JSONObject start, Date startTime, Date endTime,
                          ShieldStatisticEvent statisticEvent, List<ShieldRejectionEvent> rejectionEvents )
    {
        this.start = start;
        this.startTime = startTime;
        this.endTime = endTime;
        this.statisticEvent = statisticEvent;
        this.rejectionEvents = rejectionEvents;
    }

    public JSONObject getStart()
    {
        return this.start;
    }

    public Date getStartTime()
    {
        return this.startTime;
    }

    public Date getEndTime()
    {
        return this.endTime;
    }

    public ShieldStatisticEvent getStatisticEvent()
    {
        return this.statisticEvent;
    }

    
    public List<ShieldRejectionEvent> getRejectionEvents()
    {
        return this.rejectionEvents;
    }
    
    static LogIteration parse( JSONObject iteration_js ) throws JSONException
    {
        JSONObject start = iteration_js.getJSONObject( "start_time" );
        Date startTime = parseDate( start );
        Date endTime = parseDate( iteration_js.getJSONObject( "end_time" ));
        
        ShieldStatisticEvent statisticEvent = parseStatisticEvent( iteration_js );
        List<ShieldRejectionEvent>  rejectionEvents = new LinkedList<ShieldRejectionEvent>();

        JSONObject user_js = iteration_js.getJSONObject( "user" );
        if ( user_js == null ) user_js = new JSONObject();

        String users[] = JSONObject.getNames( user_js );
        if ( users == null ) users = new String[0];
        for ( String user : users ) {
            try {
                InetAddress address = IPAddress.parse( user ).getAddr();
                
                parseUserData( rejectionEvents, address, user_js.getJSONArray( user ));
            } catch ( ParseException e ) {
                /* Ignore corrupted IP Addresses. */
                continue;
            } catch ( UnknownHostException e ) {
                /* Ignore corrupted IP Addresses. */
                continue;
            }
        }
        
        return new LogIteration( start, startTime, endTime, statisticEvent, rejectionEvents );
    }

    private static ShieldStatisticEvent parseStatisticEvent( JSONObject iteration_js ) throws JSONException
    {
        ShieldStatisticEvent event = new ShieldStatisticEvent();
        JSONArray globals_js = iteration_js.getJSONArray( "globals" );
        int length = 0;

        int accepted = 0;
        int limited = 0;
        int dropped = 0;
        int rejected = 0;
        
        if (( length = globals_js.length()) > 0 ) {
            for ( int c = 0 ; c < length ; c++ ) {
                JSONArray temp_js = globals_js.getJSONArray( c );
                if ( temp_js.length() != 6 ) continue;
                rejected += temp_js.getInt( 1 );
                dropped += temp_js.getInt( 2 );
                limited += temp_js.getInt( 3 );
                accepted += temp_js.getInt( 4 );
                /* errors are ignored. */
            }

            event.setAccepted( accepted );
            event.setLimited( limited );
            event.setDropped( dropped );
            event.setRejected( rejected );
        }

        JSONObject mode_js = iteration_js.getJSONObject( "mode" );
        event.setRelaxed( mode_js.optInt( "relaxed", 0 ));
        event.setLax( mode_js.optInt( "lax", 0 ));
        event.setTight( mode_js.optInt( "tight", 0 ));
        event.setClosed( mode_js.optInt( "closed", 0 ));
        
        return event;
    }

    private static void parseUserData( List<ShieldRejectionEvent>  rejectionEvents, InetAddress address, JSONArray user_js ) throws JSONException
    {
        double reputation = user_js.getDouble( 0 );

        for ( int c = 1 ; c < user_js.length(); c++ ) {
            JSONArray temp_js = user_js.getJSONArray( c );
            if ( temp_js.length() != 6 ) continue;
            
            /* Mark it as internal */
            InterfaceConfiguration clientIntf = null;

            try {
                clientIntf = UvmContextFactory.context().networkManager().getNetworkConfiguration().findBySystemName( temp_js.optString( 0, "" ));
            } catch ( Exception e ) {
                clientIntf = UvmContextFactory.context().networkManager().getNetworkConfiguration().findByName("Internal");
            }

            int mode = 0;
            int clientIntfId = clientIntf.getInterfaceId();
            int limited = temp_js.getInt( 3 );
            int dropped = temp_js.getInt( 2 );
            int rejected = temp_js.getInt( 1 );

            rejectionEvents.add( new ShieldRejectionEvent( address, clientIntfId, reputation, mode, limited, dropped, rejected ));
        }
    }
    
    private static Date parseDate( JSONObject date ) throws JSONException
    {
        if ( !"timeval".equals( date.getString( "type" ))) throw new JSONException( "invalid date." );
        
        /* Time in millis */
        long time = date.getLong( "tv_sec" ) * 1000L;
        time += date.getLong( "tv_usec" ) / 1000L;

        return new Date( time );
    }
}
