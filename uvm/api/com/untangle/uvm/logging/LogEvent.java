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

import org.json.JSONObject;
import org.json.JSONString;

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

    /**
     * Time the event was logged, as filled in by logger.
     */
    public Timestamp getTimeStamp() { return timeStamp; }
    public void setTimeStamp( Timestamp timeStamp ) { this.timeStamp = timeStamp; }

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
    
    public abstract PreparedStatement getDirectEventSql( Connection conn ) throws Exception;

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
    
    public String getTag()
    {
        return this.tag;
    }

    public void setTag(String tag)
    {
        this.tag = tag;
    }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
