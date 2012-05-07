/*
 * $Id: LogEventFromReports.java 30553 2011-12-22 00:21:46Z dmorris $
 */
package com.untangle.uvm.logging;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

/**
 * A log event and message.
 * This is the base log event for most all events that untangle apps log to the database
 */
@SuppressWarnings("serial")
@MappedSuperclass
public abstract class LogEventFromReports implements Comparable<LogEventFromReports>, Serializable
{
    public static final int DEFAULT_STRING_SIZE = 255;

    private Date timeStamp = new Date();
    private String tag;

    protected LogEventFromReports() { }

    private String id;
    @Id
    @Column(name="event_id")
    @GeneratedValue
    public String getId() { return id; }
    public void setId( String id ) { this.id = id; }
    
    /**
     * Time the event was logged, as filled in by logger.
     *
     * @return time logged.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="time_stamp")
    public Date getTimeStamp()
    {
        return timeStamp;
    }

    /**
     * Don't make Aaron angry!  This should only be set by the event
     * logging system unless you're doing tricky things (with Aaron's
     * approval).
     */
    public void setTimeStamp(Date timeStamp)
    {
        if (timeStamp instanceof Timestamp) {
            this.timeStamp = new Date(timeStamp.getTime());
        } else {
            this.timeStamp = timeStamp;
        }
    }

    public int compareTo(LogEventFromReports le)
    {
        int i = -timeStamp.compareTo(le.getTimeStamp());
        if (0 == i) {
            if (le.id == id) {
                return 0;
            } else {
                String t = null == id ? "0" : id;
                String u = null == le.id ? "0" : le.id;
                return t.compareTo(u);
            }
        } else {
            return i;
        }
    }
}
