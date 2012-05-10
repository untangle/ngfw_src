/**
 * $Id$
 */
package com.untangle.uvm.logging;

import java.io.Serializable;
import java.sql.Timestamp;
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
    
    private Date timeStamp = new Date();
    private String tag; /* syslog tag */

    protected LogEvent() { }

    /**
     * Time the event was logged, as filled in by logger.
     */
    public Date getTimeStamp()
    {
        return timeStamp;
    }

    /**
     * 
     */
    public void setTimeStamp(Date timeStamp)
    {
        if (timeStamp instanceof Timestamp) {
            this.timeStamp = new Date(timeStamp.getTime());
        } else {
            this.timeStamp = timeStamp;
        }
    }

    public abstract String getDirectEventSql();

    /**
     * Default just returns one item with the result of getDirectEventSql
     */
    public List<String> getDirectEventSqls()
    {
        LinkedList<String> newList = new LinkedList<String>();
        String sql = getDirectEventSql();
        if (sql != null)
            newList.add(sql);
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
