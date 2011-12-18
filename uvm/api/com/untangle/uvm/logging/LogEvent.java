/*
 * $Id$
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
 *
 * Hibernate mappings for this class are in the UVM resource
 * directory.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
@MappedSuperclass
public abstract class LogEvent implements Comparable<LogEvent>, Serializable
{
    public static final int DEFAULT_STRING_SIZE = 255;

    private String id;
    private Date timeStamp = new Date();
    private String tag;

    protected LogEvent() { }

    @Id
    @Column(name="event_id")
    @GeneratedValue
    public String getId()
    {
        return id;
    }

    protected void setId(String id)
    {
        this.id = id;
    }

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

    /**
     * LogEvents inserted into the database and syslog when this method
     * returns true.
     *
     * @return true when this event is saved to the database.
     */
    @Transient
    public boolean isPersistent()
    {
        return true;
    }

    public abstract void appendSyslog(SyslogBuilder a);

    @Transient
    public String getSyslogId()
    {
        String[] s = getClass().getName().split("\\.");

        return s[s.length - 1];
    }

    @Transient
    public SyslogPriority getSyslogPriority()
    {
        return SyslogPriority.INFORMATIONAL; // statistics or normal operation
    }

    @Transient
    public String getTag()
    {
        return this.tag;
    }

    @Transient
    public void setTag(String tag)
    {
        this.tag = tag;
    }
    
    public int compareTo(LogEvent le)
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
