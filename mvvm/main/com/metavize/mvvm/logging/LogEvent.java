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
public abstract class LogEvent implements Serializable
{
    private static final long serialVersionUID = 3836086272903683391L;

    // How big a varchar() do we get for default String fields.  This
    // should be elsewhere. XXX
    public static final int DEFAULT_STRING_SIZE = 255;

    private Long id;
    private Date timeStamp;

    // constructors -----------------------------------------------------------

    protected LogEvent() { }

    // abstract methods -------------------------------------------------------

    // XXX make abstract
    //public abstract String toSyslog();
    public String toSyslog()
    {
        return "";
    }

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
}
