/**
 * $Id$
 */
package com.untangle.uvm.logging;

import java.io.Serializable;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.util.Map;
import java.util.Date;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.UvmContextFactory;

/**
 * A log event and message.
 * This is the base log event for most all events that untangle apps log to the database
 */
@SuppressWarnings("serial")
public abstract class LogEvent implements Serializable, JSONString
{
    protected static final Logger logger = Logger.getLogger(LogEvent.class);

    protected static String schemaPrefix = "reports.";
    protected static boolean partitionsSupported = true;
    
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
    
    public abstract void compileStatements( Connection conn, Map<String,PreparedStatement> statementCache ) throws Exception;
    public abstract String toSummaryString();

    /**
     * This either grabs a statement for a previous query from the cache
     * or creates (and stores) one
     */
    public PreparedStatement getStatementFromCache( String sqlString, Map<String,PreparedStatement> cache, Connection conn )
    {
        String key = this.getClass().getName() + "," + sqlString;
        
        PreparedStatement cachedStatement = cache.get( key );
        if ( cachedStatement != null )
            return cachedStatement;

        // otherwise create one and put it in the cache
        PreparedStatement statement;
        try {
            statement = conn.prepareStatement( sqlString );
        } catch (Exception e) {
            logger.warn("Failed to compile SQL: " + sqlString, e);
            return null;
        }
        cache.put( key, statement );
        return statement;
    }
    
    public String getPartitionTablePostfix()
    {
        if ( !partitionsSupported )
            return "";
        
        return getPartitionTablePostfix( this.timeStamp );
    }

    public String getPartitionTablePostfix( Timestamp ts )
    {
        if ( !partitionsSupported )
            return "";

        Calendar cal = UvmContextFactory.context().systemManager().getCalendar();

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
        JSONObject jO = toJSONObject();
        if ( jO == null)
            return null;
        return jO.toString();
    }

    public JSONObject toJSONObject()
    {
        JSONObject jsonObject = null;

        // we should use a better serializer
        return new JSONObject(this);

        // the jabsorb serializer is also bad for serializing dates and things
        // try {
        //     String json = UvmContextFactory.context().getSerializer().toJSON(this);
        //     jsonObject = new JSONObject(json);
        //     return jsonObject;
        // } catch (Exception e) {
        //     logger.warn("Failed to serialize JSON: " + this, e);
        //     return null;
        // }
    }

    public static void setSchemaPrefix( String newValue )
    {
        schemaPrefix = newValue;
    }

    public static String schemaPrefix()
    {
        return schemaPrefix;
    }

    public static void setPartitionsSupported( boolean newValue )
    {
        partitionsSupported = newValue;
    }
    
    public static boolean partitionsSupported()
    {
        return partitionsSupported;
    }
    
}
