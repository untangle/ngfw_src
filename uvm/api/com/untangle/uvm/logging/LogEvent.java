/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.logging;

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
 * Hibernate mappings for this class are in the MVVM resource
 * directory.
 */
@MappedSuperclass
public abstract class LogEvent implements Comparable, Serializable
{
    public static final int DEFAULT_STRING_SIZE = 255;

    private Long id;
    private Date timeStamp = new Date();

    // constructors -----------------------------------------------------------

    protected LogEvent() { }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="event_id")
    @GeneratedValue
    public Long getId()
    {
        return id;
    }

    protected void setId(Long id)
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

    // public methods ---------------------------------------------------------

    /**
     * LogEvents inserted into the database when this method returns
     * true.
     *
     * @return true when this event is saved to the database.
     */
    @Transient
    public boolean isPersistent()
    {
        return true;
    }

    // Syslog methods ---------------------------------------------------------

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

    // Comparable methods -----------------------------------------------------

    public int compareTo(Object o)
    {
        LogEvent le = (LogEvent)o;

        int i = -timeStamp.compareTo(le.getTimeStamp());
        if (0 == i) {
            if (le.id == id) {
                return 0;
            } else {
                Long t = null == id ? 0L : id;
                Long u = null == le.id ? 0L : le.id;
                return t.compareTo(u);
            }
        } else {
            return i;
        }
    }
}
