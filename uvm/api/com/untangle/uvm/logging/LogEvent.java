/**
 * $Id$
 */
package com.untangle.uvm.logging;

import java.io.Serializable;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.Calendar;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.UvmContextFactory;

/**
 * A log event and message.
 * This is the base log event for most all events that untangle apps log to the database
 */
public abstract class LogEvent implements Serializable, JSONString
{
    public static final int DEFAULT_STRING_SIZE = 255;
    
    protected Timestamp timeStamp = new Timestamp((new Date()).getTime());
    private String tag; /* syslog tag */

    protected LogEvent() { }

    public Timestamp getTimeStamp() { return timeStamp; }
    public void setTimeStamp( Timestamp timeStamp ) { this.timeStamp = timeStamp; }

    public String getTag() { return this.tag; }
    public void setTag( String newValue ) { this.tag = newValue; }

    public Timestamp timeStampPlusHours( int hours )
    {
        long time = this.timeStamp.getTime();
        time += hours*60*60*1000;
        return new Timestamp(time);
    }

    public Timestamp timeStampPlusMinutes( int min )
    {
        long time = this.timeStamp.getTime();
        time += min*60*1000;
        return new Timestamp(time);
    }

    public Timestamp timeStampPlusSeconds( int sec )
    {
        long time = this.timeStamp.getTime();
        time += sec*1000;
        return new Timestamp(time);
    }
    
    public abstract PreparedStatement getDirectEventSql( Connection conn ) throws Exception;
    public abstract String toSummaryString();

    /**
     * Default just returns one item with the result of getDirectEventSql
     */
    public List<PreparedStatement> getDirectEventSqls( Connection conn ) throws Exception
    {
        LinkedList<PreparedStatement> newList = new LinkedList<PreparedStatement>();
        PreparedStatement pstmt = getDirectEventSql( conn );
        if (pstmt != null)
            newList.add(pstmt);
        return newList;
    }
    
    public String getPartitionTablePostfix()
    {
        return getPartitionTablePostfix( this.timeStamp );
    }

    public String getPartitionTablePostfix( Timestamp ts )
    {
        Calendar cal = UvmContextFactory.context().adminManager().getCalendar();

        // in theory this should be synchronized
        // but I'm worried about the performance of synchronizing this
        // the penalty for accidently getting another events timestamp through concurrency
        // is low so I'm just going to leave the synchronization disabled for now
        //synchronized( cal ) {

        cal.setTime( ts );
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH)+1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return "_" + year + "_" + String.format("%02d",month) + "_" + String.format("%02d",day);

        //}
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public JSONObject toJSONObject()
    {
        JSONObject jO = new JSONObject(this);
        return jO;
    }
}
