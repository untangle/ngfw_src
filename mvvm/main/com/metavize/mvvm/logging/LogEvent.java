/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: LogEvent.java,v 1.11 2005/02/25 02:45:29 amread Exp $
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

    private Long id;
    private Date timeStamp;

    // constructors -----------------------------------------------------------

    protected LogEvent() { }

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="EVENT_ID"
     * generator-class="native"
     */
    protected Long getId()
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
