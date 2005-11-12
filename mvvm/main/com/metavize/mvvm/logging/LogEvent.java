/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.logging;

import java.io.Serializable;
import java.util.Date;

/**
 * A log event and message.
 *
 * Hibernate mappings for this class are in the MVVM resource
 * directory.
 */
public abstract class LogEvent implements Comparable, Serializable
{
    // XXX wrong! each class should have a serial UID
    private static final long serialVersionUID = 3836086272903683391L;

    // How big a varchar() do we get for default String fields.  This
    // should be elsewhere. XXX
    public static final int DEFAULT_STRING_SIZE = 255;

    private Long id;
    private Date timeStamp = new Date();

    // constructors -----------------------------------------------------------

    protected LogEvent() { }

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="EVENT_ID"
     * generator-class="native"
     */
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
     * @hibernate.property
     * column="TIME_STAMP"
     */
    public Date getTimeStamp()
    {
        return timeStamp;
    }

    void setTimeStamp(Date timeStamp)
    {
        this.timeStamp = timeStamp;
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder a) { }

    public String getSyslogId()
    {
        String[] s = getClass().getName().split("\\.");

        return s[s.length - 1];
    }

    public SyslogPriority getSyslogPrioritiy()
    {
        return SyslogPriority.INFORMATIONAL;
    }

    // Comparable methods -----------------------------------------------------

    public int compareTo(Object o)
    {
        LogEvent le = (LogEvent)o;

        int i = timeStamp.compareTo(le.timeStamp);

        if (0 == i) {
            if (le.id == id) {
                return 0;
            } else {
                Integer t = System.identityHashCode(this);
                Integer u = System.identityHashCode(le);
                return t.compareTo(u);
            }
        } else {
            return i;
        }
    }
}
